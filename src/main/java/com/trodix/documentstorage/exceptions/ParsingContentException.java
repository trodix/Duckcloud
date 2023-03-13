package com.trodix.documentstorage.exceptions;

public class ParsingContentException extends RuntimeException {

    public ParsingContentException(String msg, Exception e) {
        super(msg, e);
    }

    public ParsingContentException(Exception e) {
        super(e);
    }

}
