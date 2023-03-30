package fr.usmb.m1isc.compilation.tp;

import java.util.ArrayList;

public class CompilArbre {
    private ArrayList<Object> children;
    private String action;

    public CompilArbre(String action) {
        this.action = action;
        this.children = new ArrayList<>();
    }

    public void addChild(Object child) {
        this.children.add(child);
    }

    public ArrayList<Object> getChildren() {
        return children;
    }

    @Override
    public String toString() {
        // String str = "(" + this.action;
        // for (Object child : children) {
        // if (child != null) str += " " + child.toString();
        // else str += " ()";
        // }
        // return str + ")";
        if (this.action == "EXPR" || this.action == "NUMBER" || this.action == "IDENT" || this.action == "PAREN") {
            return this.children.get(0).toString();
        }
        String str = "(" + CompilArbre.actionToPrintable(this.action);
        for (Object child : children) {
            if (child != null)
                str += " " + child.toString();
            else
                str += " ()";
        }
        return str + ")";
    }

    private static String actionToPrintable(String action) {
        switch (action) {
            case "PLUS":
                return "+";
            case "MOINS":
                return "-";
            case "MUL":
                return "*";
            case "DIV":
                return "/";
            case "MOD":
                return "%";
            case "MINUS":
                return "-";
            case "IF":
                return "IF";
            case "WHILE":
                return "WHILE";
            case "LET":
                return "LET";
            case "EGAL":
                return "==";
            case "GT":
                return ">";
            case "GTE":
                return ">=";
            case "AND":
                return "&&";
            case "OR":
                return "||";
            case "NOT":
                return "!";
            case ";":
                return ";";
            default:
                return "";
        }
    }
}
