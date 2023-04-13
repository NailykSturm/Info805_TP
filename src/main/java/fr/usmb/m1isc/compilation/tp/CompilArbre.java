package fr.usmb.m1isc.compilation.tp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class CompilArbre {
    private final ArrayList<Object> children;
    private final String action;

    public CompilArbre(String action) {
        this.action = action;
        this.children = new ArrayList<>();
    }

    private Set<String> getHeaderRecc() {
        Set<String> vars = new HashSet<>();
        if (Objects.equals(this.action, "IDENT") || Objects.equals(this.action, "LET")) {
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
                if (!(this.children.get(1) instanceof CompilArbre)) return getCodeLeftBranch();
                return String.format("%s%s",
                        getCodeLeftBranch(),
                        getCodeRightBranch());
            case "LET":
                return String.format("%s\tmov %s, eax\n",
                        getCodeRightBranch(),
                        this.children.get(0).toString());
            case "NUMBER":
            case "IDENT":
                return String.format("\tmov eax, %s\n",
                        this.children.get(0).toString());
            case "PLUS":
                return String.format("%s\tpush eax\n%s\tpop ebx\n\tadd eax, ebx\n",
                        getCodeLeftBranch(),
                        getCodeRightBranch());
            case "MOINS":
                return String.format("%s\tpush eax\n%s\tpop ebx\n\tsub eax, ebx\n",
                        getCodeLeftBranch(),
                        getCodeRightBranch());
            case "MINUS":
                return String.format("%s\tmul eax, -1\n",
                        getCodeLeftBranch());
            case "MUL":
                return String.format("%s\tpush eax\n%s\tpop ebx\n\tmul eax, ebx\n",
                        getCodeLeftBranch(),
                        getCodeRightBranch());
            case "DIV":
                return String.format("%s\tpush eax\n%s\tpop ebx\n\tdiv ebx\n\tmov eax, ebx\n",
                        getCodeLeftBranch(),
                        getCodeRightBranch());
            case "MOD":
                return String.format("%s\tpush eax\n%s\tpop ebx\n\txor edx,edx\n\tdiv ebx\n\tmov eax, edx\n",
                        getCodeLeftBranch(),
                        getCodeRightBranch());
            case "EXPR":
            case "PAREN":
                return String.format("%s",
                        getCodeLeftBranch());
            case "INPUT":
                return "\tin eax\n";
            case "OUTPUT":
                return String.format("%s\tout eax\n",
                        getCodeLeftBranch());
            case "WHILE":
                idx = whileIdx++;
                return String.format(
                        "debut_while_%1$d:\n%2$s\tjz sortie_while_%1$d\n%3$s\tjmp debut_while_%1$d\nsortie_while_%1$d:\n",
                        idx, getCodeLeftBranch(),
                        getCodeRightBranch());
            case "GT":
                idx = ifIdx++;
                return String.format(
                        "%2$s\tpush eax\n%3$s\tpop ebx\n\tsub eax, ebx\n\tjle faux_gt_%1$d\n\tmov eax, 1\n\tjmp sortie_gt_%1$d\nfaux_gt_%1$d:\n\tmov eax, 0\nsortie_gt_%1$d:\n",
                        idx, getCodeLeftBranch(),
                        getCodeRightBranch());
            case "IF":
                idx = ifIdx++;
                return String.format("%2$s\tjnz si_else_%1$d\n%3$s\tjmp si_fin_%1$d\nsi_else_%1$d:\n%4$ssi_fin_%1$d:\n",
                        idx, getCodeLeftBranch(),
                        getCodeRightBranch(),
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

    private String getCodeRightBranch() {
        return getCodeFromBranch(1);
    }

    private String getCodeFromBranch(int branchId) {
        return ((CompilArbre) this.children.get(branchId)).getCodeRecc();
    }

    private String getCodeLeftBranch() {
        return getCodeFromBranch(0);
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
        if (Objects.equals(this.action, "EXPR") || Objects.equals(this.action, "NUMBER") || Objects.equals(this.action, "IDENT") || Objects.equals(this.action, "PAREN")) {
            return this.children.get(0).toString();
        }
        StringBuilder str = new StringBuilder("(" + CompilArbre.actionToPrintable(this.action));
        for (Object child : children) {
            if (child != null)
                str.append(" ").append(child);
            else
                str.append(" ()");
        }
        return str + ")";
    }

    public String toStringLarge() {
        StringBuilder str = new StringBuilder("(" + this.action);
        for (Object child : children) {
            if (child != null) {
                if (child instanceof CompilArbre) {
                    str.append(" ").append(((CompilArbre) child).toStringLarge());
                } else
                    str.append(" ").append(child);
            }
            else
                str.append(" ()");
        }
        return str + ")";
    }

    private static String actionToPrintable(String action) {
        switch (action) {
            case "PLUS":
                return "+";
            case "MOINS":
            case "MINUS":
                return "-";
            case "MUL":
                return "*";
            case "DIV":
                return "/";
            case "MOD":
                return "%";
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
