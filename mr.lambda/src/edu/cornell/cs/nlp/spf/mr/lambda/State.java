package edu.cornell.cs.nlp.spf.mr.lambda;

import edu.cornell.cs.nlp.spf.mr.lambda.mapping.ScopeMapping;
import it.unimi.dsi.fastutil.objects.ReferenceSets;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class State<T extends Variable> {
    private Set<T> state;

    public Set<T> getState() {
        return state;
    }

    public void add(T elem) {
        state.add(elem);
    }

    public State() {
        this(new HashSet<>());
    }

    public State(Set<T> initialState) {
        state = initialState;
    }

    public void addAll(Set<T> elems) {
        state.addAll(elems);
    }

    public void add(State<T> state) {
        this.addAll(state.getState());
    }

    @Override
	public int hashCode() {
        final int prime = 31;
        int result = 1;
        for (T i : state) {
            result = prime * result + (i == null ? 0 : i.hashCode());
        }
        return result;
    }

    public boolean equals(State<T> other, ScopeMapping<Variable, Variable> mapping) {
        boolean found;
        for (T a : state) {
            found = false;
            for (T b : other.state) {
                if (a.equals(b, mapping)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }
}
