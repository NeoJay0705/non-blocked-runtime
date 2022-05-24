package org.easylibs.exception;

public class UndeclaredIOTypeException extends Exception {
    public UndeclaredIOTypeException(String IOType) {
        super("This IO type \"" + IOType + "\" is not declared in advance");
    }
}
