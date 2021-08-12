package com.mojagap.mojanode.model.wallet;

import com.mojagap.mojanode.model.common.AuditEntity;
import com.mojagap.mojanode.model.transaction.TransactionStatus;
import com.mojagap.mojanode.model.transaction.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;

@Setter
@Entity(name = "wallet_transaction")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction extends AuditEntity {
    private Wallet wallet;
    private TransactionType transactionType;
    private BigDecimal amount;
    private TransactionStatus transactionStatus;
    private WalletTransactionRequest walletTransactionRequest;
    private BankDepositTransaction bankDepositTransaction;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "wallet_id")
    public Wallet getWallet() {
        return wallet;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    public TransactionType getTransactionType() {
        return transactionType;
    }

    @Column(name = "amount")
    public BigDecimal getAmount() {
        return amount;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_status")
    public TransactionStatus getTransactionStatus() {
        return transactionStatus;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_transaction_request_id")
    public WalletTransactionRequest getWalletTransactionRequest() {
        return walletTransactionRequest;
    }

    @JoinColumn(name = "bank_deposit_transaction_id")
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    public BankDepositTransaction getBankDepositTransaction() {
        return bankDepositTransaction;
    }
}
