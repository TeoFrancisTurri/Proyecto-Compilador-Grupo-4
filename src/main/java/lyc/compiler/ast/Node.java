package lyc.compiler.ast;

import java.util.ArrayList;
import java.util.List;
import lyc.compiler.symboltable.DataType;

public class Node {
    
    private NodeType type;
    private String value;
    private DataType dataType; 
    private List<Node> children;

    public Node(NodeType type, String value, DataType dataType) {
        this.type = type;
        this.value = value;
        this.dataType = dataType;
        this.children = new ArrayList<>();
    }

    public Node(NodeType type, String value) {
        this(type, value, null);
    }

    public Node(NodeType type) {
        this(type, null, null);
    }

    public void addChild(Node child) {
        if (child != null) {
            children.add(child);
        }
    }

    // Getters
    public NodeType getType()        { return type; }
    public String getValue()         { return value; }
    public DataType getDataType()    { return dataType; }
    public List<Node> getChildren()  { return children; }
    public Node getChild(int i)      { return children.get(i); }
    public boolean hasChildren()     { return !children.isEmpty(); }

    // Setter
    public void setDataType(DataType dataType) { this.dataType = dataType; }

    // Para debug
    public void print(String indent) {
        System.out.println(indent + type 
            + (value != null ? " [" + value + "]" : "")
            + (dataType != null ? " (" + dataType + ")" : ""));
        for (Node child : children) {
            child.print(indent + "  ");
        }
    }
}