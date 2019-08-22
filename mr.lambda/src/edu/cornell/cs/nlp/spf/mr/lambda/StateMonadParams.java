package edu.cornell.cs.nlp.spf.mr.lambda;

public class StateMonadParams implements MonadParams {

    State<SkolemId> state;
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

    public State<SkolemId> getState(){
        return state;
    }
}
