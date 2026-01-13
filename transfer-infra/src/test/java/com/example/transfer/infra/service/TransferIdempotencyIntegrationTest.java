package com.example.transfer.infra.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.transfer.domain.dto.TransferResult;
import com.example.transfer.domain.entity.Account;
import com.example.transfer.domain.exception.IdempotentRequestInProgressException;
import com.example.transfer.domain.repository.IdempotencyRecordRepository;
import com.example.transfer.domain.service.AccountCommandService;
import com.example.transfer.domain.service.AccountQueryService;
import com.example.transfer.domain.service.TransferCommandFacade;
import com.example.transfer.infra.config.IntegrationTestSupport;

@DisplayName("멱등성 통합 테스트")
class TransferIdempotencyIntegrationTest extends IntegrationTestSupport {

	@Autowired
	private AccountCommandService accountCommandService;

	@Autowired
	private AccountQueryService accountQueryService;

	@Autowired
	private TransferCommandFacade transferCommandFacade;

	@Autowired
	private IdempotencyRecordRepository idempotencyRecordRepository;

	@Test
	@DisplayName("같은 멱등키로 순차 요청 시 첫 번째만 처리되고 이후는 캐시된 결과 반환")
	void sameIdempotencyKey_sequentialRequests_processedOnce() {
		// given
		Account sender = accountCommandService.createAccount("멱등성-보내는이");
		Account receiver = accountCommandService.createAccount("멱등성-받는이");
		accountCommandService.deposit(sender.getId(), 1_000_000L);

		String idempotencyKey = UUID.randomUUID().toString();
		long transferAmount = 100_000L;

		// when - 첫 번째 요청
		TransferResult firstResult = transferCommandFacade.transfer(
			sender.getAccountNo(), receiver.getAccountNo(), transferAmount, idempotencyKey
		);

		// 두 번째 요청 (같은 멱등키)
		TransferResult secondResult = transferCommandFacade.transfer(
			sender.getAccountNo(), receiver.getAccountNo(), transferAmount, idempotencyKey
		);

		// 세 번째 요청 (같은 멱등키)
		TransferResult thirdResult = transferCommandFacade.transfer(
			sender.getAccountNo(), receiver.getAccountNo(), transferAmount, idempotencyKey
		);

		// then - 모두 동일한 결과
		assertThat(firstResult.transferId()).isEqualTo(secondResult.transferId());
		assertThat(secondResult.transferId()).isEqualTo(thirdResult.transferId());
		assertThat(firstResult.amount()).isEqualTo(secondResult.amount());
		assertThat(firstResult.fee()).isEqualTo(secondResult.fee());
		assertThat(firstResult.occurredAt()).isEqualTo(secondResult.occurredAt());

		// 잔액 확인 - 한 번만 처리됨
		Account senderAfter = accountQueryService.getAccount(sender.getId());
		Account receiverAfter = accountQueryService.getAccount(receiver.getId());

		long expectedFee = (long)Math.floor(transferAmount * 0.01);
		assertThat(senderAfter.getBalanceWon()).isEqualTo(1_000_000L - transferAmount - expectedFee);
		assertThat(receiverAfter.getBalanceWon()).isEqualTo(transferAmount);
	}

	@Test
	@DisplayName("다른 멱등키로 요청 시 각각 별도 처리")
	void differentIdempotencyKeys_processedSeparately() {
		// given
		Account sender = accountCommandService.createAccount("멱등성-보내는이2");
		Account receiver = accountCommandService.createAccount("멱등성-받는이2");
		accountCommandService.deposit(sender.getId(), 1_000_000L);

		String key1 = UUID.randomUUID().toString();
		String key2 = UUID.randomUUID().toString();
		long transferAmount = 100_000L;

		// when
		TransferResult result1 = transferCommandFacade.transfer(
			sender.getAccountNo(), receiver.getAccountNo(), transferAmount, key1
		);
		TransferResult result2 = transferCommandFacade.transfer(
			sender.getAccountNo(), receiver.getAccountNo(), transferAmount, key2
		);

		// then - 서로 다른 transferId
		assertThat(result1.transferId()).isNotEqualTo(result2.transferId());

		// 잔액 확인 - 두 번 처리됨
		Account senderAfter = accountQueryService.getAccount(sender.getId());
		Account receiverAfter = accountQueryService.getAccount(receiver.getId());

		long expectedFee = (long)Math.floor(transferAmount * 0.01);
		long totalDeduction = (transferAmount + expectedFee) * 2;
		assertThat(senderAfter.getBalanceWon()).isEqualTo(1_000_000L - totalDeduction);
		assertThat(receiverAfter.getBalanceWon()).isEqualTo(transferAmount * 2);
	}

	@Test
	@DisplayName("동시에 같은 멱등키로 요청 시 한 번만 처리되고 나머지는 예외 또는 캐시 결과")
	void sameIdempotencyKey_concurrentRequests_processedOnce() throws InterruptedException {
		// given
		Account sender = accountCommandService.createAccount("동시멱등-보내는이");
		Account receiver = accountCommandService.createAccount("동시멱등-받는이");
		accountCommandService.deposit(sender.getId(), 1_000_000L);

		String idempotencyKey = UUID.randomUUID().toString();
		long transferAmount = 100_000L;

		int workerCount = 10;
		ExecutorService executor = Executors.newFixedThreadPool(workerCount);
		CountDownLatch ready = new CountDownLatch(workerCount);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(workerCount);

		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger inProgressCount = new AtomicInteger();
		List<UUID> transferIds = new ArrayList<>();

		// when
		for (int i = 0; i < workerCount; i++) {
			executor.submit(() -> {
				ready.countDown();
				try {
					start.await();
					TransferResult result = transferCommandFacade.transfer(
						sender.getAccountNo(), receiver.getAccountNo(), transferAmount, idempotencyKey
					);
					synchronized (transferIds) {
						transferIds.add(result.transferId());
					}
					successCount.incrementAndGet();
				} catch (IdempotentRequestInProgressException e) {
					inProgressCount.incrementAndGet();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					done.countDown();
				}
			});
		}

		ready.await(5, TimeUnit.SECONDS);
		start.countDown();
		done.await(10, TimeUnit.SECONDS);
		executor.shutdownNow();

		// then
		// 성공한 요청들은 모두 같은 transferId를 가져야 함
		if (!transferIds.isEmpty()) {
			UUID firstTransferId = transferIds.get(0);
			assertThat(transferIds).allMatch(id -> id.equals(firstTransferId));
		}

		// 실제 이체는 한 번만 처리됨
		Account senderAfter = accountQueryService.getAccount(sender.getId());
		Account receiverAfter = accountQueryService.getAccount(receiver.getId());

		long expectedFee = (long)Math.floor(transferAmount * 0.01);
		assertThat(senderAfter.getBalanceWon()).isEqualTo(1_000_000L - transferAmount - expectedFee);
		assertThat(receiverAfter.getBalanceWon()).isEqualTo(transferAmount);

		// 멱등성 레코드 확인
		assertThat(idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey)).isPresent();
	}
}
