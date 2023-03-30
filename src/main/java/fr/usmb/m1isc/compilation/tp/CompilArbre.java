package fr.usmb.m1isc.compilation.tp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

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

    private Set<String> getHeaderRecc() {
        Set<String> vars = new HashSet<>();
        if (this.action == "IDENT" || this.action == "LET") {
            vars.add(this.children.get(0).toString());
        }
        for (Object child : children) {
            if (child instanceof CompilArbre) {
                vars.addAll(((CompilArbre) child).getHeaderRecc());
            }
        }
        return vars;
    }

    private String getHeader() {
        Set<String> vars = this.getHeaderRecc();
        String header = "DATA SEGMENT\n";
        for (String v : vars) {
            header = String.format("%s\t%s DD\n", header, v);
        }
        return String.format("%sDATA ENDS\n", header);
    }

    private static int whileIdx = 1;
    private static int ifIdx = 1;

    private String getCodeRecc() {
        int idx;
        switch (action) {
            case ";":
                if (!(this.children.get(1) instanceof CompilArbre)) return ((CompilArbre) this.children.get(0)).getCodeRecc();
                return String.format("%s%s",
                        ((CompilArbre) this.children.get(0)).getCodeRecc(),
                        ((CompilArbre) this.children.get(1)).getCodeRecc());
            case "LET":
                return String.format("%s\tmov %s, eax\n",
                        ((CompilArbre) this.children.get(1)).getCodeRecc(),
                        this.children.get(0).toString());
            case "NUMBER":
                return String.format("\tmov eax, %s\n",
                        this.children.get(0).toString());
            case "IDENT":
                return String.format("\tmov eax, %s\n",
                        this.children.get(0).toString());
            case "PLUS":
                return String.format("%s\tpush eax\n%s\tpop ebx\n\tadd eax, ebx\n",
                        ((CompilArbre) this.children.get(0)).getCodeRecc(),
                        ((CompilArbre) this.children.get(1)).getCodeRecc());
            case "MOINS":
                return String.format("%s\tpush eax\n%s\tpop ebx\n\tsub eax, ebx\n",
                        ((CompilArbre) this.children.get(0)).getCodeRecc(),
                        ((CompilArbre) this.children.get(1)).getCodeRecc());
            case "MINUS":
                return String.format("%s\tmul eax, -1\n",
                        ((CompilArbre) this.children.get(0)).getCodeRecc());
            case "MUL":
                return String.format("%s\tpush eax\n%s\tpop ebx\n\tmul eax, ebx\n",
                        ((CompilArbre) this.children.get(0)).getCodeRecc(),
                        ((CompilArbre) this.children.get(1)).getCodeRecc());
            case "DIV":
                return String.format("%s\tpush eax\n%s\tpop ebx\n\tdiv ebx\n\tmov eax, ebx\n",
                        ((CompilArbre) this.children.get(0)).getCodeRecc(),
                        ((CompilArbre) this.children.get(1)).getCodeRecc());
            case "MOD":
                return String.format("%s\tpush eax\n%s\tpop ebx\n\txor edx,edx\n\tdiv ebx\n\tmov eax, edx\n",
                        ((CompilArbre) this.children.get(0)).getCodeRecc(),
                        ((CompilArbre) this.children.get(1)).getCodeRecc());
            case "EXPR":
                return String.format("%s",
                        ((CompilArbre) this.children.get(0)).getCodeRecc());
            case "PAREN":
                return String.format("%s",
                        ((CompilArbre) this.children.get(0)).getCodeRecc());
            case "INPUT":
                return String.format("\tin eax\n");
            case "OUTPUT":
                return String.format("%s\tout eax\n",
                        ((CompilArbre) this.children.get(0)).getCodeRecc());
            case "WHILE":
                idx = whileIdx++;
                return String.format(
                        "debut_while_%1$d:\n%2$s\tjz sortie_while_%1$d\n%3$s\tjmp debut_while_%1$d\nsortie_while_%1$d:\n",
                        idx, ((CompilArbre) this.children.get(0)).getCodeRecc(),
                        ((CompilArbre) this.children.get(1)).getCodeRecc());
            case "GT":
                idx = ifIdx++;
                return String.format(
                        "%2$s\tpush eax\n%3$s\tpop ebx\n\tsub eax, ebx\n\tjle faux_gt_%1$d\n\tmov eax, 1\n\tjmp sortie_gt_%1$d\nfaux_gt_%1$d:\n\tmov eax, 0\nsortie_gt_%1$d:\n",
                        idx, ((CompilArbre) this.children.get(0)).getCodeRecc(),
                        ((CompilArbre) this.children.get(1)).getCodeRecc());
            case "IF":
                idx = ifIdx++;
                return String.format("%2$s\tjnz si_else_%1$d\n%3$s\tjmp si_fin_%1$d\nsi_else_%1$d:\n%4$ssi_fin_%1$d:\n",
                        idx, ((CompilArbre) this.children.get(0)).getCodeRecc(),
                        ((CompilArbre) this.children.get(1)).getCodeRecc(),
                        ((CompilArbre) this.children.get(2)).getCodeRecc());
            case "NOT":
                return String.format("");
            case "OR":
                return String.format("");
            case "AND":
                return String.format("");
            case "EGAL":
                return String.format("");
            case "GTE":
                return String.format("");
            case "NIL":
                return String.format("");
            default:
                return String.format("; undefined action: %s", this.action);
        }
    }

    private String getCode() {
        return String.format("CODE SEGMENT\n%sCODE ENDS\n", this.getCodeRecc());
    }

    public String treeToCode() {
        return getHeader() + getCode();
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

    public String toStringLarge() {
        String str = "(" + this.action;
        for (Object child : children) {
            if (child != null) {
                if (child instanceof CompilArbre) {
                    str += " " + ((CompilArbre) child).toStringLarge();
                } else
                    str += " " + child.toString();
            }
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
            case "OUTPUT":
                return "OUTPUT";
            case "INPUT":
                return "INPUT";
            default:
                return "";
        }
    }
}
