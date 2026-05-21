package com.codegym.finance.service.wallet;

import com.codegym.finance.entity.wallet.Wallet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class WalletServiceTest {

    @Autowired
    private IWalletService walletService;

    @Test
    void testServiceInjected() {
        assertNotNull(walletService, "WalletService should be injected");
    }

    @Test
    void testFindByUsername() {
        // This test depends on your initial data or you can create one
        List<Wallet> wallets = walletService.findByUsername("admin");
        assertNotNull(wallets);
    }
}
