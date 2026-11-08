package com.example.elibrary.loan.domain;

import com.example.elibrary.common.error.DomainException;
import com.example.elibrary.common.error.ErrorCode;
import java.util.UUID;

public class LoanAlreadyActiveException extends DomainException {

    public LoanAlreadyActiveException(UUID bookId) {
        super(
                ErrorCode.LOAN_ALREADY_ACTIVE,
                "An active loan for book " + bookId + " already exists for this user");
    }
}
