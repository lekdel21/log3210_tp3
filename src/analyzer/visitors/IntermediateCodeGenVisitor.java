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
public class IntermediateCodeGenVisitor implements ParserVisitor {
    private final PrintWriter m_writer;

    public HashMap<String, VarType> SymbolTable = new HashMap<>();
    public HashMap<String, Integer> EnumValueTable = new HashMap<>();

    private int id = 0;
    private int label = 0;

    public IntermediateCodeGenVisitor(PrintWriter writer) {
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
        VarType varType;

        if (node.getValue() == null) {
            varName = ((ASTIdentifier) node.jjtGetChild(1)).getValue();
            varType = VarType.EnumVar;
        } else
            varType = node.getValue().equals("num") ? VarType.Number : VarType.Bool;

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
        SymbolTable.put(((ASTIdentifier) node.jjtGetChild(0)).getValue(), VarType.EnumType);
        for (int i = 1; i < node.jjtGetNumChildren(); i++) {
            EnumValueTable.put(((ASTIdentifier) node.jjtGetChild(i)).getValue(), i - 1);
        }
        return null;
    }

    @Override
    public Object visit(ASTSwitchStmt node, Object data) {
        // TODO

        String switchVar = (String) node.jjtGetChild(0).jjtAccept(this, data);
        String nextLabel;
        String gotoLabel = "";

        for (int i = 1; i < node.jjtGetNumChildren(); i++) {
            if (i != node.jjtGetNumChildren() - 1) nextLabel = newLabel();
            else nextLabel = "_L0";
            m_writer.println("if " + switchVar + " != " + EnumValueTable.get((String) node.jjtGetChild(i).jjtAccept(this, data)) + " goto " + nextLabel);
            if (!gotoLabel.equals("")) {
                m_writer.println(gotoLabel);
                gotoLabel = "";
            }
            node.jjtGetChild(i).jjtGetChild(1).jjtAccept(this, data);
            if (node.jjtGetChild(i).jjtGetNumChildren() == 3) node.jjtGetChild(i).jjtGetChild(2).jjtAccept(this, data);
            else if (i != node.jjtGetNumChildren() - 1) {
                gotoLabel = newLabel();
                m_writer.println("goto " + gotoLabel);
            }
            if (i != node.jjtGetNumChildren() - 1) m_writer.println(nextLabel);
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
            BoolLabel boolLabel = new BoolLabel(newLabel(), nextLabel);
            node.jjtGetChild(0).jjtAccept(this, boolLabel);
            m_writer.println(boolLabel.lTrue);
            node.jjtGetChild(1).jjtAccept(this, nextLabel);
        } else {
            BoolLabel boolLabel = new BoolLabel(newLabel(), newLabel());
            node.jjtGetChild(0).jjtAccept(this, boolLabel);
            m_writer.println(boolLabel.lTrue);
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
        BoolLabel boolLabel = new BoolLabel(newLabel(), nextLabel);
        m_writer.println(start);
        node.jjtGetChild(0).jjtAccept(this, boolLabel);
        m_writer.println(boolLabel.lTrue);
        node.jjtGetChild(1).jjtAccept(this, start);
        m_writer.println("goto " + start);
        return null;
    }

    @Override
    public Object visit(ASTForStmt node, Object data) {
        // TODO
        String nextLabel = data != null ? ((BoolLabel) data).lTrue : "_L0";
        String start = newLabel();
        BoolLabel boolLabel = new BoolLabel(newLabel(), nextLabel);
        BoolLabel startBoolLabel = new BoolLabel(newLabel(), nextLabel);
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
            BoolLabel boolLabel = new BoolLabel(newLabel(), newLabel());
            node.jjtGetChild(1).jjtAccept(this, boolLabel);
            m_writer.println(boolLabel.lTrue + "\n" + identifier + " = 1");
            String nextId = data != null ? data.toString() : "_L0";
            m_writer.println("goto " + nextId + "\n" + boolLabel.lFalse + "\n" + identifier + " = 0");
        } else {
            m_writer.println(identifier + " = " + EnumValueTable.get(node.jjtGetChild(1).jjtAccept(this, data)));
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
                    BoolLabel boolLabel = new BoolLabel(newLabel(), ((BoolLabel) data).lFalse);
                    node.jjtGetChild(i).jjtAccept(this, boolLabel);
                    m_writer.println(boolLabel.lTrue);
                } else if (op.equals("||")) {
                    BoolLabel boolLabel = new BoolLabel(((BoolLabel) data).lTrue, newLabel());
                    node.jjtGetChild(i).jjtAccept(this, boolLabel);
                    m_writer.println(boolLabel.lFalse);
                }
            } else {
                node.jjtGetChild(i).jjtAccept(this, data);
            }
        }
        return null;
    }

    @Override
    public Object visit(ASTCompExpr node, Object data) {
        // TODO
        if (node.jjtGetNumChildren() != 1) {
            m_writer.println("if " + node.jjtGetChild(0).jjtAccept(this, data) + " " + node.getValue() + " "
                    + node.jjtGetChild(1).jjtAccept(this, data) + " goto " + ((BoolLabel) data).lTrue
            );
            m_writer.println("goto " + ((BoolLabel) data).lFalse);
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
        if (node.getValue()) m_writer.println("goto " + ((BoolLabel) data).lTrue);
        else m_writer.println("goto " + ((BoolLabel) data).lFalse);
        return null;
    }

    @Override
    public Object visit(ASTIdentifier node, Object data) {
        // TODO
        if (SymbolTable.get(node.getValue()) == VarType.Bool) {
            m_writer.println("if " + node.getValue() + " == 1 goto " + ((BoolLabel) data).lTrue);
            m_writer.println("goto " + ((BoolLabel) data).lFalse);
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
