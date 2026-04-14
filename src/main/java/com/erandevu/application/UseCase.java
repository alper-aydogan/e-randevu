package com.erandevu.application;

/**
 * Clean Architecture Use Case abstraction.
 * All application use cases must implement this interface.
 *
 * @param <I> Input type (Command/Query)
 * @param <O> Output type (Response)
 */
@FunctionalInterface
public interface UseCase<I, O> {

    /**
     * Execute the use case with the given input.
     *
     * @param input the use case input
     * @return the use case output
     */
    O execute(I input);
}

/**
 * Use Case for commands that don't return a value.
 */
@FunctionalInterface
interface CommandUseCase<I> {
    void execute(I input);
}

/**
 * Use Case for queries that don't require input.
 */
@FunctionalInterface
interface QueryUseCase<O> {
    O execute();
}

/**
 * Use Case that requires no input and returns no output.
 */
@FunctionalInterface
interface VoidUseCase {
    void execute();
}
