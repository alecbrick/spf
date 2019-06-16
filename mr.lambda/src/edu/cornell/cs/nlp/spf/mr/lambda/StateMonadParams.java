package edu.cornell.cs.nlp.spf.mr.lambda;

public class StateMonadParams implements MonadParams {

    State state;
    LogicalExpression body;

    public StateMonadParams() {
        this(new State<>(), null);
    }

    public StateMonadParams(State state, LogicalExpression body) {
        this.state = state;
        this.body = body;
    }

    @Override
    public LogicalExpression getOutput() {
        return body;
    }

    public State getState(){
        return state;
    }
}
