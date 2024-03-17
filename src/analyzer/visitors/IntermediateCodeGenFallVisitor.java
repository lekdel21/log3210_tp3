package analyzer.visitors;

import analyzer.ast.*;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Vector;


/**
 * Ce visiteur explore l'AST et génère du code intermédiaire.
 *
 * @author Félix Brunet
 * @author Doriane Olewicki
 * @author Quentin Guidée
 * @author Raphaël Tremblay
 * @version 2024.02.26
 */
public class IntermediateCodeGenFallVisitor implements ParserVisitor {
    public static final String FALL = "fall";

    private final PrintWriter m_writer;

    public HashMap<String, VarType> SymbolTable = new HashMap<>();
    public HashMap<String, Integer> EnumValueTable = new HashMap<>();

    private int id = 0;
    private int label = 0;

    public IntermediateCodeGenFallVisitor(PrintWriter writer) {
        m_writer = writer;
    }

    private String newID() {
        return "_t" + id++;
    }

    private String newLabel() {
        return "_L" + label++;
    }

    @Override
    public Object visit(SimpleNode node, Object data) {
        return data;
    }

    @Override
    public Object visit(ASTProgram node, Object data) {
        // TODO
        String endLabel = newLabel();
        node.childrenAccept(this, data);
        m_writer.println(endLabel);
        return null;
    }

    @Override
    public Object visit(ASTDeclaration node, Object data) {
        String varName = ((ASTIdentifier) node.jjtGetChild(0)).getValue();
        IntermediateCodeGenFallVisitor.VarType varType;

        if (node.getValue() == null) {
            varName = ((ASTIdentifier) node.jjtGetChild(1)).getValue();
            varType = IntermediateCodeGenFallVisitor.VarType.EnumVar;
        } else
            varType = node.getValue().equals("num") ? IntermediateCodeGenFallVisitor.VarType.Number : IntermediateCodeGenFallVisitor.VarType.Bool;

        SymbolTable.put(varName, varType);
        return null;
    }

    @Override
    public Object visit(ASTBlock node, Object data) {
        // TODO
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            if (i != node.jjtGetNumChildren() - 1) {
                String label = newLabel();
                node.jjtGetChild(i).jjtAccept(this, label);
                m_writer.println(label);
            } else {
                node.jjtGetChild(i).jjtAccept(this, data);
            }
        }
        return null;
    }

    @Override
    public Object visit(ASTEnumStmt node, Object data) {
        // TODO
        SymbolTable.put(((ASTIdentifier) node.jjtGetChild(0)).getValue(), IntermediateCodeGenFallVisitor.VarType.EnumType);
        for (int i = 1; i < node.jjtGetNumChildren(); i++) {
            EnumValueTable.put(((ASTIdentifier) node.jjtGetChild(i)).getValue(), i - 1);
        }
        return null;
    }

    @Override
    public Object visit(ASTSwitchStmt node, Object data) {
        // TODO

        String switchVar = (String) node.jjtGetChild(0).jjtAccept(this, data);
        String[] labels = new String[node.jjtGetNumChildren() * 2 - 1];
        String[] caseVars = new String[node.jjtGetNumChildren() - 1];

        int j = 0;
        String label = "";
        for (int i = 0; i < labels.length - 1; i += 2) {
            labels[i] = newLabel();
            caseVars[j] = (String) node.jjtGetChild(j + 1).jjtAccept(this, data);
            m_writer.println("if " + switchVar + " == " + EnumValueTable.get(caseVars[j]) + " goto " + labels[i]);
            if (i == labels.length - 3) m_writer.println("goto _L0");
            else {
                labels[i + 1] = newLabel();
                m_writer.println("goto " + labels[i + 1]);
            }
            m_writer.println(labels[i]);
            if (!label.equals("")) {
                m_writer.println(label);
                label = "";
            }
            j++;
            node.jjtGetChild(j).jjtGetChild(1).jjtAccept(this, data);
            if (node.jjtGetChild(j).jjtGetNumChildren() < 3 && i != labels.length - 3) {
                label = newLabel();
                m_writer.println("goto " + label);
            }

            if (node.jjtGetChild(j).jjtGetNumChildren() == 3) node.jjtGetChild(j).jjtGetChild(2).jjtAccept(this, data);
            if (i != labels.length - 3) m_writer.println(labels[i + 1]);
        }

        return null;
    }

    @Override
    public Object visit(ASTCaseStmt node, Object data) {
        // TODO
        String value = (String) node.jjtGetChild(0).jjtAccept(this, data);
        return value;
    }

    @Override
    public Object visit(ASTBreakStmt node, Object data) {
        // TODO
        m_writer.println("goto _L0");
        return null;
    }

    @Override
    public Object visit(ASTStmt node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTIfStmt node, Object data) {
        // TODO
        String nextLabel = data != null ? (String) data : "_L0";
        if (node.jjtGetNumChildren() == 2) {
            BoolLabel boolLabel = new BoolLabel("fall", nextLabel);
            node.jjtGetChild(0).jjtAccept(this, boolLabel);
            node.jjtGetChild(1).jjtAccept(this, nextLabel);
        } else {
            BoolLabel boolLabel = new BoolLabel("fall", newLabel());
            node.jjtGetChild(0).jjtAccept(this, boolLabel);
            node.jjtGetChild(1).jjtAccept(this, nextLabel);
            m_writer.println("goto " + nextLabel + "\n" + boolLabel.lFalse);
            node.jjtGetChild(2).jjtAccept(this, nextLabel);
        }
        return null;
    }

    @Override
    public Object visit(ASTWhileStmt node, Object data) {
        // TODO
        String nextLabel = data != null ? (String) data : "_L0";
        String start = newLabel();
        BoolLabel boolLabel = new BoolLabel("fall", nextLabel);
        m_writer.println(start);
        node.jjtGetChild(0).jjtAccept(this, boolLabel);
        node.jjtGetChild(1).jjtAccept(this, start);
        m_writer.println("goto " + start);
        return null;
    }

    @Override
    public Object visit(ASTForStmt node, Object data) {
        // TODO
        String nextLabel = data != null ? ((IntermediateCodeGenFallVisitor.BoolLabel) data).lTrue : "_L0";
        String start = newLabel();
        IntermediateCodeGenFallVisitor.BoolLabel boolLabel = new IntermediateCodeGenFallVisitor.BoolLabel(newLabel(), nextLabel);
        IntermediateCodeGenFallVisitor.BoolLabel startBoolLabel = new IntermediateCodeGenFallVisitor.BoolLabel(newLabel(), nextLabel);
        node.jjtGetChild(0).jjtAccept(this, boolLabel);
        m_writer.println(start);
        node.jjtGetChild(1).jjtAccept(this, startBoolLabel);
        m_writer.println(startBoolLabel.lTrue);
        node.jjtGetChild(3).jjtAccept(this, boolLabel);
        m_writer.println(boolLabel.lTrue);
        node.jjtGetChild(2).jjtAccept(this, boolLabel);
        m_writer.println("goto " + start);
        return null;
    }

    @Override
    public Object visit(ASTAssignStmt node, Object data) {
        // TODO
        String identifier = ((ASTIdentifier) node.jjtGetChild(0)).getValue();
        if (SymbolTable.get(identifier) == VarType.Number) {
            m_writer.println(identifier + " = " + node.jjtGetChild(1).jjtAccept(this, data));
        } else if (SymbolTable.get(identifier) == VarType.Bool) {
            BoolLabel boolLabel = new BoolLabel(FALL, newLabel());
            node.jjtGetChild(1).jjtAccept(this, boolLabel);
            m_writer.println(identifier + " = 1");
            String nextId = data != null ? data.toString() : "_L0";
            m_writer.println("goto " + nextId + "\n" + boolLabel.lFalse + "\n" + identifier + " = 0");
        } else {
            m_writer.println(identifier + " = " + EnumValueTable.get((String) node.jjtGetChild(1).jjtAccept(this, data)));
        }
        return identifier;
    }

    @Override
    public Object visit(ASTExpr node, Object data) {
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    public Object codeExtAddMul(SimpleNode node, Object data, Vector<String> ops) {
        // À noter qu'il n'est pas nécessaire de boucler sur tous les enfants.
        // La grammaire n'accepte plus que 2 enfants maximum pour certaines opérations, au lieu de plusieurs
        // dans les TPs précédents. Vous pouvez vérifier au cas par cas dans le fichier Grammaire.jjt.

        // TODO
        if (node.jjtGetNumChildren() == 1) {
            return node.jjtGetChild(0).jjtAccept(this, data);
        } else {
            String addr = newID();
            String value1 = (String) node.jjtGetChild(0).jjtAccept(this, data);
            String value2 = (String) node.jjtGetChild(1).jjtAccept(this, data);
            String res = addr + " = " + value1 + " " + ops.firstElement() + " " + value2;
            m_writer.println(res);
            return addr;
        }
    }

    @Override
    public Object visit(ASTAddExpr node, Object data) {
        return codeExtAddMul(node, data, node.getOps());
    }

    @Override
    public Object visit(ASTMulExpr node, Object data) {
        return codeExtAddMul(node, data, node.getOps());
    }

    @Override
    public Object visit(ASTUnaExpr node, Object data) {
        // TODO
        Vector<String> operators = node.getOps();
        if (operators.isEmpty()) {
            return node.jjtGetChild(0).jjtAccept(this, data);
        }
        String addr = "";
        for (int i = 0; i < operators.size(); i++) {
            String tmp = "";
            String op = operators.get(i);
            if (i == 0) {
                String res = (String) node.jjtGetChild(0).jjtAccept(this, data);
                tmp = newID();
                m_writer.println(tmp + " = " + op + " " + res);
            } else {
                tmp = newID();
                m_writer.println(tmp + " = " + op + " " + addr);
            }
            addr = tmp;
        }
        return addr;
    }

    @Override
    public Object visit(ASTBoolExpr node, Object data) {
        // TODO
        if (node.jjtGetNumChildren() == 1) {
            return node.jjtGetChild(0).jjtAccept(this, data);
        }
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            if (i % 2 == 0) {
                String op = "";
                if (i == 0) op = (String) node.getOps().get(0);
                else op = (String) node.getOps().get(i - 1);

                if (op.equals("&&")) {
                    if (((BoolLabel) data).lFalse.equals(FALL)) {
                        BoolLabel boolLabel = new BoolLabel(FALL, newLabel());
                        node.jjtGetChild(i).jjtAccept(this, boolLabel);
                        node.jjtGetChild(i + 1).jjtAccept(this, data);
                        m_writer.println(boolLabel.lFalse);
                    } else {
                        BoolLabel boolLabel = new BoolLabel(FALL, ((BoolLabel) data).lFalse);
                        node.jjtGetChild(i).jjtAccept(this, boolLabel);
                        node.jjtGetChild(i + 1).jjtAccept(this, data);
                    }
                } else if (op.equals("||")) {
                    if (((BoolLabel) data).lTrue.equals(FALL)) {
                        BoolLabel boolLabel = new BoolLabel(newLabel(), FALL);
                        node.jjtGetChild(i).jjtAccept(this, boolLabel);
                        node.jjtGetChild(i + 1).jjtAccept(this, data);
                        m_writer.println(boolLabel.lTrue);
                    } else {
                        BoolLabel boolLabel = new BoolLabel(((BoolLabel) data).lTrue, FALL);
                        node.jjtGetChild(i).jjtAccept(this, boolLabel);
                        node.jjtGetChild(i + 1).jjtAccept(this, data);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Object visit(ASTCompExpr node, Object data) {
        // TODO
        if (node.jjtGetNumChildren() != 1) {
            m_writer.println("ifFalse " + node.jjtGetChild(0).jjtAccept(this, data) + " " + node.getValue() + " "
                    + node.jjtGetChild(1).jjtAccept(this, data) + " goto " + ((BoolLabel) data).lFalse
            );
            //m_writer.println("goto " + ((BoolLabel) data).lFalse);
            return null;
        }
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    @Override
    public Object visit(ASTNotExpr node, Object data) {
        if (node.getOps().size() % 2 == 0) {
            return node.jjtGetChild(0).jjtAccept(this, data);
        } else {
            BoolLabel bl = new BoolLabel(((BoolLabel) data).lFalse, ((BoolLabel) data).lTrue);
            return node.jjtGetChild(0).jjtAccept(this, bl);
        }
    }

    @Override
    public Object visit(ASTGenValue node, Object data) {
        // TODO
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    @Override
    public Object visit(ASTBoolValue node, Object data) {
        // TODO
        if (node.getValue() && !((BoolLabel) data).lTrue.equals(FALL)) {
            m_writer.println("goto " + ((BoolLabel) data).lTrue);
        } else if (!node.getValue() && !((BoolLabel) data).lFalse.equals(FALL)) {
            System.out.println(((BoolLabel) data).lFalse);
            m_writer.println("goto " + ((BoolLabel) data).lFalse);
        }
        return null;
    }

    @Override
    public Object visit(ASTIdentifier node, Object data) {
        // TODO
        if (SymbolTable.get(node.getValue()) == VarType.Bool) {
            BoolLabel lData = (BoolLabel) data;
            if (!lData.lTrue.equals(FALL) && !lData.lFalse.equals(FALL)) {
                m_writer.println("if " + node.getValue() + " == 1 goto " + lData.lTrue);
                m_writer.println("goto " + lData.lFalse);
            } else if (!lData.lTrue.equals(FALL) && lData.lFalse.equals(FALL)) {
                m_writer.println("if " + node.getValue() + " == 1 goto " + lData.lTrue);
            } else if (lData.lTrue.equals(FALL) && !lData.lFalse.equals(FALL)) {
                m_writer.println("ifFalse " + node.getValue() + " == 1 goto " + lData.lFalse);
            }
        }

        return node.getValue();
    }

    @Override
    public Object visit(ASTIntValue node, Object data) {
        return Integer.toString(node.getValue());
    }

    public enum VarType {
        Bool,
        Number,
        EnumType,
        EnumVar,
        EnumValue
    }

    private static class BoolLabel {
        public String lTrue;
        public String lFalse;

        public BoolLabel(String lTrue, String lFalse) {
            this.lTrue = lTrue;
            this.lFalse = lFalse;
        }
    }
}
