package com.bank.transfer;

import com.bank.transfer.client.AccountsClient;
import com.bank.transfer.client.BlockerClient;
import com.bank.transfer.client.ExchangeClient;
import com.bank.transfer.client.NotificationClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = "spring.profiles.active=test")
class TransferApplicationTests {

  @MockBean
  private AccountsClient accountsClient;

  @MockBean
  private BlockerClient blockerClient;

  @MockBean
  private ExchangeClient exchangeClient;

  @MockBean
  private NotificationClient notificationClient;

  @Test
  void contextLoads() {}
}
