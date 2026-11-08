package com.example.elibrary.loan.domain;

import com.example.elibrary.common.error.DomainException;
import com.example.elibrary.common.error.ErrorCode;
import java.util.UUID;

public class LoanNotFoundException extends DomainException {

    public LoanNotFoundException(UUID loanId) {
        super(ErrorCode.LOAN_NOT_FOUND, "Loan " + loanId + " was not found");
    }
}
