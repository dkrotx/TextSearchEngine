package org.bytesoft.tsengine.qparse;

public class ExprToken {
    public enum Type {
        Operator, Bracket, Word;
    }

    private final Type type;
    private final Object value;

    public ExprToken(Type t, Object val) {
        type = t;
        value = val;
    }

    public boolean IsOperator() {
        return type == Type.Operator;
    }

    public boolean IsBracket() {
        return type == Type.Bracket;
    }

    public boolean IsWord() {
        return type == Type.Word;
    }

    public char GetSymbol() {
        return (Character)value;
    }

    public char GetBracket() {
        assert(IsBracket());
        return (Character)value;
    }

    public char GetOperator() {
        assert(IsOperator());
        return (Character)value;
    }

    public String GetWord() {
        assert(IsWord());
        return (String)value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object obj) {
        ExprToken b = (ExprToken)obj;
        return type == b.type && value.equals(b.value);
    }
}