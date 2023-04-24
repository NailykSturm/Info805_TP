package fr.usmb.m1isc.compilation.tp;

import java.io.BufferedWriter;
import java.io.FileWriter;
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

    public void addChild(CompilArbre c) {
        this.children.add(c);
    }

    public void addChild(String s) {
        this.children.add(s);
    }

    public void addChild(Integer i) {
        this.children.add(i);
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

    private static int idx = 0;

    private String getCodeRecc() {
        switch (action) {

            //////////////////////
            // BASIC OPERATORS
            //////////////////////
            case ";":
                if (!(this.children.get(1) instanceof CompilArbre)) return getCodeLeftBranch();
                return getCodeLeftBranch() + getCodeRightBranch();
            case "LET":
                return String.format("%s\tmov %s, eax\n",
                        getCodeRightBranch(),
                        this.children.get(0).toString());
            case "NUMBER":
            case "IDENT":
                return String.format("\tmov eax, %s\n",
                        this.children.get(0).toString());
            case "NIL":
                // représentation courante du null / nil en asm
                return "\tmov eax, 0\n";

            //////////////////////
            // UNARY OPERATIONS
            //////////////////////
            case "EXPR":
            case "PAREN":
                return getCodeLeftBranch();
            case "MINUS":
                return String.format("%s\tmul eax, -1\n",
                        getCodeLeftBranch());

            //////////////////////
            // DUAL OPERATIONS
            //////////////////////
            case "PLUS":
                return String.format("%s\tpush eax\n%s\tpop ebx\n\tadd eax, ebx\n",
                        getCodeLeftBranch(),
                        getCodeRightBranch());
            case "MOINS":
                return String.format("%s\tpush eax\n%s\tpop ebx\n\tsub ebx, eax\n\tmov eax, ebx\n",
                        getCodeLeftBranch(),
                        getCodeRightBranch());
            case "MUL":
                return String.format("%s\tpush eax\n%s\tpop ebx\n\tmul eax, ebx\n",
                        getCodeLeftBranch(),
                        getCodeRightBranch());
            case "DIV":
                return String.format("%s\tpush eax\n%s\tpop ebx\n\tdiv ebx, eax\n\tmov eax, ebx\n",
                        getCodeLeftBranch(),
                        getCodeRightBranch());
            case "MOD":
                return String.format("%s\tpush eax\n%s\tpop ebx\n\tmov ecx,ebx\n\tdiv ecx,eax\n\tmul ecx,eax\n\tsub ebx,ecx\n\tmov eax,ebx\n",
                        getCodeLeftBranch(),
                        getCodeRightBranch());

            //////////////////////
            // IO FUNCTIONS
            //////////////////////
            case "INPUT":
                return "\tin eax\n";
            case "OUTPUT":
                return getCodeLeftBranch() + "\tout eax\n";

            //////////////////////
            // CONDITIONNAL BLOCS
            //////////////////////
            case "WHILE":
                idx++;
                return String.format(
                        "debut_while_%1$d:\n%2$s\tjz sortie_while_%1$d\n%3$s\tjmp debut_while_%1$d\nsortie_while_%1$d:\n",
                        idx, getCodeLeftBranch(),
                        getCodeRightBranch());
            case "IF":
                idx++;
                return String.format("%2$s\tjnz si_else_%1$d\n%3$s\tjmp si_fin_%1$d\nsi_else_%1$d:\n%4$ssi_fin_%1$d:\n",
                        idx, getCodeLeftBranch(),
                        getCodeRightBranch(),
                        ((CompilArbre) this.children.get(2)).getCodeRecc());

            //////////////////////
            // BOOLEAN OPERATIONS
            //////////////////////
            case "GT":
                idx++;
                return String.format(
                        "%2$s\tpush eax\n%3$s\tpop ebx\n\tsub eax, ebx\n\tjle faux_gt_%1$d\n\tmov eax, 1\n\tjmp sortie_gt_%1$d\nfaux_gt_%1$d:\n\tmov eax, 0\nsortie_gt_%1$d:\n",
                        idx, getCodeLeftBranch(),
                        getCodeRightBranch());
            case "NOT":
                // Logique du not en asm : inversion bit à bit. Si on recoit en entrée un 0/1, ca nous sort -1/-2. En rajoutant 2, on se retrouve avec 1/0, ce qui inverse bien le booléen.
                return String.format("%1$s\tnot eax\n\tadd eax, 2\n", getCodeLeftBranch());
            case "OR":
                idx++;
                return String.format("%2$s\tpush eax\n%3$s\tpop ebx\n\tor eax, ebx\n\tjz faux_or_%1$d\n\tmov eax, 1\n\tjmp sortie_or_%1$d\nfaux_or_%1$d:\n\tmov eax, 0\nsortie_or_%1$d:\n",
                        idx, getCodeLeftBranch(),
                        getCodeRightBranch());
            case "AND":
                idx++;
                return String.format("%2$s\tpush eax\n%3$s\tpop ebx\n\tand eax, ebx\n\tjz faux_and_%1$d\n\tmov eax, 1\n\tjmp sortie_and_%1$d\nfaux_and_%1$d:\n\tmov eax, 0\nsortie_and_%1$d:\n",
                        idx, getCodeLeftBranch(),
                        getCodeRightBranch());
            case "EGAL":
                idx++;
                return String.format(
                        "%2$s\tpush eax\n%3$s\tpop ebx\n\tsub eax, ebx\n\tjnz faux_eq_%1$d\n\tmov eax, 1\n\tjmp sortie_eq_%1$d\nfaux_eq_%1$d:\n\tmov eax, 0\nsortie_eq_%1$d:\n",
                        idx, getCodeLeftBranch(),
                        getCodeRightBranch());
            case "GTE":
                idx++;
                return String.format(
                        "%2$s\tpush eax\n%3$s\tpop ebx\n\tsub eax, ebx\n\tjl faux_gte_%1$d\n\tmov eax, 1\n\tjmp sortie_gte_%1$d\nfaux_gte_%1$d:\n\tmov eax, 0\nsortie_gte_%1$d:\n",
                        idx, getCodeLeftBranch(),
                        getCodeRightBranch());

            //////////////////////////////////////////////////////////////////////////////
            // ERROR CASE : HAPPEN IF SOMETHING IS DETECTED AS A WORD WHILE NOT BEING ONE
            //////////////////////////////////////////////////////////////////////////////
            default:
                return "; undefined action: " + this.action;
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
        String s = getHeader() + getCode();
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("output.asm"));
            writer.write(s);
            writer.close();
        } catch (Exception ignored) {}
        return s;
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
