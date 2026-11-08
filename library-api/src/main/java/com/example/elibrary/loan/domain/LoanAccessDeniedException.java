package com.example.elibrary.loan.domain;

import com.example.elibrary.shared.error.DomainException;
import com.example.elibrary.shared.error.ErrorCode;

public class LoanAccessDeniedException extends DomainException {

    public LoanAccessDeniedException() {
        super(ErrorCode.ACCESS_DENIED, "You are not allowed to access this loan");
    }
}
