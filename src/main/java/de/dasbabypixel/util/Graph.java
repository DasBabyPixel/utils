package de.dasbabypixel.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public interface Graph<NodeDataType, WayDataType> extends Iterable<Graph.Node<NodeDataType, WayDataType>> {

    static <NodeDataType, WayDataType> Graph<NodeDataType, WayDataType> linkedGraph() {
        return new LinkedGraph<>();
    }

    static <NodeDataType, WayDataType> Graph<NodeDataType, WayDataType> arrayGraph() {
        return new ArrayGraph<>();
    }

    @Override
    default Iterator<Node<NodeDataType, WayDataType>> iterator() {
        return nodes().iterator();
    }

    Collection<Node<NodeDataType, WayDataType>> nodes();

    Collection<Node.Connection<NodeDataType, WayDataType>> connections();

    Node<NodeDataType, WayDataType> newNode(NodeDataType data);

    void removeNode(Node<NodeDataType, WayDataType> node);

    Node.Connection<NodeDataType, WayDataType> newConnection(Node<NodeDataType, WayDataType> from, Node<NodeDataType, WayDataType> to, WayDataType way);

    void removeConnection(Node.Connection<NodeDataType, WayDataType> connection);

    default void writeAdjacencyMatrix(PrintStream writer) {
        List<Node<NodeDataType, WayDataType>> nodes = new ArrayList<>(nodes());
        int[] sizes = new int[nodes.size()];
        int max = 0;
        for (int index = 0; index < nodes.size(); index++) {
            sizes[index] = nodes.get(index).data().toString().length();
            max = Math.max(max, sizes[index] + 1);
        }
        writer.println("Graph (" + nodes.size() + " Nodes):");
        StringBuilder sb2 = new StringBuilder();
        sb2.append(String.format("%1$-" + max + "s", ""));
        for (Node<NodeDataType, WayDataType> node : nodes) {
            sb2.append(" ").append(node.data().toString());
        }
        writer.println(sb2);
        writer.printf("%1$" + max + "s", "");
        writer.print(" ");
        for (int index = 0; index < nodes.size(); index++) {
            writer.print(String
                    .format("%1$" + sizes[index] + "s", "")
                    .replace(' ', '_') + (index == nodes.size() - 1 ? " " : "_"));
        }
        writer.println();
        for (int index = 0; index < nodes.size(); index++) {
            StringBuilder sb = new StringBuilder();
            Node<NodeDataType, WayDataType> node = nodes.get(index);
            sb.append(String.format("%1$-" + max + "s", node.data().toString()));
            if (index == 0) sb.append("/");
            else if (index == nodes.size() - 1) sb.append("\\");
            else sb.append("|");
            String[] data = new String[sizes.length];
            Arrays.fill(data, "");
            for (Node.Connection<NodeDataType, WayDataType> connection : node.connections()) {
                data[nodes.indexOf(connection.to())] = connection.way().toString();
            }
            for (int dataIndex = 0; dataIndex < data.length; dataIndex++) {
                data[dataIndex] = String.format("%1$-" + sizes[dataIndex] + "s", data[dataIndex]);
                sb.append(data[dataIndex]);
                if (dataIndex == data.length - 1) if (index == 0) sb.append("\\");
                else if (index == nodes.size() - 1) sb.append("/");
                else sb.append("|");
                else sb.append("|");
            }
            writer.println(sb);
        }
        writer.printf("%1$" + max + "s", "");
        writer.print(" ");
        for (int index = 0; index < nodes.size(); index++) {
            writer.print(String
                    .format("%1$" + sizes[index] + "s", "")
                    .replace(" ", "‾") + (index == nodes.size() - 1 ? " " : "‾"));
        }
        writer.println();
    }

    default <AlgorithmData, CalculatedData> CalculatedData search(Algorithm.AlgorithmWithData<AlgorithmData, NodeDataType, WayDataType, CalculatedData> data) {
        return data.algorithm().search(this, data.data());
    }

    interface Node<NodeDataType, WayDataType> {
        List<Connection<NodeDataType, WayDataType>> connections();

        Connection<NodeDataType, WayDataType> newConnection(Node<NodeDataType, WayDataType> to, WayDataType way);

        void removeConnection(Connection<NodeDataType, WayDataType> connection);

        NodeDataType data();

        Graph<NodeDataType, WayDataType> graph();

        void remove();

        Collection<Node<NodeDataType, WayDataType>> reachableNodes();

        interface Connection<NodeDataType, WayDataType> {
            Node<NodeDataType, WayDataType> from();

            Node<NodeDataType, WayDataType> to();

            Graph<NodeDataType, WayDataType> graph();

            WayDataType way();

            void remove();
        }
    }


    interface PathWriter {
        PathWriter simple = new PathWriter() {
            @Override
            public void write(Path<?, ?> path, OutputStream out) throws IOException {
                write(path, new OutputStreamWriter(out, StandardCharsets.UTF_8));
            }

            @Override
            public void write(Path<?, ?> path, Writer writer) throws IOException {
                BufferedWriter bw = writer instanceof BufferedWriter ? (BufferedWriter) writer : new BufferedWriter(writer);
                if (path == null) {
                    bw.write("null\n");
                    bw.flush();
                    return;
                }
                boolean first = true;
                for (Node.Connection<?, ?> connection : path) {
                    if (first) {
                        first = false;
                        bw.append("Path [").append(connection.from().data().toString());
                    }
                    bw
                            .append(" > ")
                            .append(connection.way().toString())
                            .append(" > ")
                            .append(connection.to().data().toString());
                }
                bw.append("]\n");
                bw.flush();
            }
        };

        void write(Path<?, ?> path, OutputStream out) throws IOException;

        void write(Path<?, ?> path, Writer writer) throws IOException;
    }


    interface Path<NodeDataType, WayDataType> extends Iterable<Node.Connection<NodeDataType, WayDataType>> {

        Graph<NodeDataType, WayDataType> graph();

        Node<NodeDataType, WayDataType> last();

        Node<NodeDataType, WayDataType> first();

        default void write(PathWriter pathWriter, OutputStream out) throws IOException {
            pathWriter.write(this, out);
        }

        default void write(PathWriter pathWriter, Writer writer) throws IOException {
            pathWriter.write(this, writer);
        }

    }


    interface Algorithm<AlgorithmData, NodeDataType, WayDataType, CalculatedData> {

        static <NodeDataType, WayDataType> Algorithm<DijkstraData<NodeDataType, WayDataType>, NodeDataType, WayDataType, Path<NodeDataType, WayDataType>> dijkstra() {
            return new Algorithm<DijkstraData<NodeDataType, WayDataType>, NodeDataType, WayDataType, Path<NodeDataType, WayDataType>>() {
                @Override
                public Path<NodeDataType, WayDataType> search(Graph<NodeDataType, WayDataType> graph, DijkstraData<NodeDataType, WayDataType> data) {
                    TreeSet<Node> unchecked = new TreeSet<>();
                    unchecked.add(new Node(data.startNode(), 0));
                    Node node;
                    HashMap<Graph.Node<NodeDataType, WayDataType>, Node> usedNodes = new HashMap<>();
                    while ((node = unchecked.pollFirst()) != null) {
                        if (node.targetNode.equals(data.targetNode())) {
                            return node.createPath();
                        }
                        for (Graph.Node.Connection<NodeDataType, WayDataType> connection : node.targetNode.connections()) {
                            Graph.Node<NodeDataType, WayDataType> to = connection.to();
                            Node n = new Node(node, connection, to, node.distance + data
                                    .weightCalculator()
                                    .weight(connection));
                            if (usedNodes.containsKey(to)) {
                                Node oldNode = usedNodes.get(to);
                                if (oldNode.distance > n.distance) {
                                    unchecked.remove(oldNode);
                                    unchecked.add(n);
                                    usedNodes.put(to, n);
                                }
                                continue;
                            }
                            usedNodes.put(n.targetNode, n);
                            unchecked.add(n);
                        }
                    }
                    return null;
                }

                class Node implements Comparable<Node> {
                    private final Node previous;
                    private final Graph.Node.Connection<NodeDataType, WayDataType> previousConnection;
                    private final Graph.Node<NodeDataType, WayDataType> targetNode;
                    private final long distance;

                    public Node(Node previous, Graph.Node.Connection<NodeDataType, WayDataType> previousConnection, Graph.Node<NodeDataType, WayDataType> targetNode, long distance) {
                        this.previous = previous;
                        this.previousConnection = previousConnection;
                        this.targetNode = targetNode;
                        this.distance = distance;
                    }

                    public Node(Graph.Node<NodeDataType, WayDataType> targetNode, long distance) {
                        this(null, null, targetNode, distance);
                    }

                    private Path<NodeDataType, WayDataType> createPath() {
                        Util.SimplePath<NodeDataType, WayDataType> path = new Util.SimplePath<>(targetNode.graph());
                        Node cur = this;
                        do {
                            if (cur.previousConnection == null) {
                                break;
                            }
                            path.connections.add(0, cur.previousConnection);
                            cur = cur.previous;
                        } while (cur != null);
                        return path;
                    }

                    @Override
                    public int compareTo(Node o) {
                        int comp = Long.compare(distance, o.distance);
                        if (comp == 0) {
                            comp = Integer.compare(targetNode.hashCode(), o.targetNode.hashCode());
                        }
                        return comp;
                    }

                    @Override
                    public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;
                        Node node = (Node) o;
                        return distance == node.distance;
                    }

                    @Override
                    public int hashCode() {
                        return Objects.hash(distance);
                    }
                }
            };
        }

        CalculatedData search(Graph<NodeDataType, WayDataType> graph, AlgorithmData data);

        default AlgorithmWithData<AlgorithmData, NodeDataType, WayDataType, CalculatedData> withData(AlgorithmData data) {
            return new AlgorithmWithData<>(this, data);
        }

        class DijkstraData<NodeDataType, WayDataType> {
            private final Node<NodeDataType, WayDataType> startNode;
            private final Node<NodeDataType, WayDataType> targetNode;
            private final WeightCalculator<NodeDataType, WayDataType> weightCalculator;

            public DijkstraData(Node<NodeDataType, WayDataType> startNode, Node<NodeDataType, WayDataType> targetNode, WeightCalculator<NodeDataType, WayDataType> weightCalculator) {
                this.startNode = startNode;
                this.targetNode = targetNode;
                this.weightCalculator = weightCalculator;
            }

            public WeightCalculator<NodeDataType, WayDataType> weightCalculator() {
                return weightCalculator;
            }

            public Node<NodeDataType, WayDataType> startNode() {
                return startNode;
            }

            public Node<NodeDataType, WayDataType> targetNode() {
                return targetNode;
            }

            public interface WeightCalculator<NodeDataType, WayDataType> {
                long weight(Node.Connection<NodeDataType, WayDataType> connection);
            }
        }


        class AlgorithmWithData<AlgorithmData, NodeDataType, WayDataType, CalculatedData> {
            private final Algorithm<AlgorithmData, NodeDataType, WayDataType, CalculatedData> algorithm;
            private final AlgorithmData data;

            public AlgorithmWithData(Algorithm<AlgorithmData, NodeDataType, WayDataType, CalculatedData> algorithm, AlgorithmData data) {
                this.algorithm = algorithm;
                this.data = data;
            }

            public AlgorithmData data() {
                return data;
            }

            public Algorithm<AlgorithmData, NodeDataType, WayDataType, CalculatedData> algorithm() {
                return algorithm;
            }
        }
    }


    class LinkedGraph<NodeDataType, WayDataType> implements Graph<NodeDataType, WayDataType> {

        private final ArrayList<Node<NodeDataType, WayDataType>> nodes = new ArrayList<>();

        @Override
        public Collection<Node<NodeDataType, WayDataType>> nodes() {
            return new AbstractCollection<Node<NodeDataType, WayDataType>>() {
                @Override
                public Iterator<Node<NodeDataType, WayDataType>> iterator() {
                    return new Iterator<Node<NodeDataType, WayDataType>>() {
                        private final Iterator<Node<NodeDataType, WayDataType>> it = nodes.iterator();
                        private Node<NodeDataType, WayDataType> node;

                        @Override
                        public void remove() {
                            LinkedNode linkedNode = (LinkedNode) node;
                            while (!linkedNode.connections.isEmpty()) {
                                linkedNode.connections.get(0).remove();
                            }
                            while (!linkedNode.origins.isEmpty()) {
                                linkedNode.origins.get(0).remove();
                            }
                            it.remove();
                        }

                        @Override
                        public boolean hasNext() {
                            return it.hasNext();
                        }

                        @Override
                        public Node<NodeDataType, WayDataType> next() {
                            return node = it.next();
                        }
                    };
                }

                @Override
                public int size() {
                    return nodes.size();
                }

                @Override
                public String toString() {
                    Iterator<Node<NodeDataType, WayDataType>> it = iterator();
                    if (!it.hasNext()) return "Node$Nodes(0)[]";

                    StringBuilder sb = new StringBuilder();
                    sb.append("Node$Nodes(").append(size()).append(")[");
                    for (; ; ) {
                        Node<NodeDataType, WayDataType> e = it.next();
                        sb.append(e);
                        if (!it.hasNext()) return sb.append(']').toString();
                        sb.append(',').append(' ');
                    }
                }
            };
        }

        @Override
        public Collection<Node.Connection<NodeDataType, WayDataType>> connections() {
            return new Collection<Node.Connection<NodeDataType, WayDataType>>() {
                @Override
                public int size() {
                    int size = 0;
                    for (Node<NodeDataType, WayDataType> node : nodes) {
                        size += node.connections().size();
                    }
                    return size;
                }

                @Override
                public String toString() {
                    Iterator<Node.Connection<NodeDataType, WayDataType>> it = iterator();
                    if (!it.hasNext()) return "Node$Nodes(0)[]";

                    StringBuilder sb = new StringBuilder();
                    sb.append("Node$Nodes(").append(size()).append(")[");
                    for (; ; ) {
                        Node.Connection<NodeDataType, WayDataType> e = it.next();
                        sb.append(e);
                        if (!it.hasNext()) return sb.append(']').toString();
                        sb.append(',').append(' ');
                    }
                }

                @Override
                public boolean isEmpty() {
                    for (Node<NodeDataType, WayDataType> node : nodes) {
                        if (!node.connections().isEmpty()) return false;
                    }
                    return true;
                }

                @Override
                public boolean contains(Object o) {
                    for (Node<NodeDataType, WayDataType> node : nodes) {
                        if (node.connections().contains(o)) return true;
                    }
                    return false;
                }

                @Override
                public Iterator<Node.Connection<NodeDataType, WayDataType>> iterator() {
                    return new Iterator<Node.Connection<NodeDataType, WayDataType>>() {
                        private Iterator<Node<NodeDataType, WayDataType>> nodeIterator;
                        private Iterator<Node.Connection<NodeDataType, WayDataType>> currentIterator;
                        private Node.Connection<NodeDataType, WayDataType> currentConnection;

                        @Override
                        public void remove() {
                            if (currentConnection == null) throw new IllegalStateException();

                            currentIterator.remove();
                            LinkedNode to = (LinkedNode) currentConnection.to();
                            to.origins.remove(currentConnection);

                            currentConnection = null;
                        }

                        @Override
                        public boolean hasNext() {
                            return currentIterator() != null;
                        }

                        @Override
                        public Node.Connection<NodeDataType, WayDataType> next() {
                            Iterator<Node.Connection<NodeDataType, WayDataType>> cit = currentIterator();
                            if (cit == null) throw new NoSuchElementException();
                            return currentConnection = cit.next();
                        }

                        private Iterator<Node.Connection<NodeDataType, WayDataType>> currentIterator() {
                            do {
                                if (currentIterator != null && currentIterator.hasNext()) return currentIterator;
                                if (nodeIterator().hasNext()) {
                                    Node<NodeDataType, WayDataType> node = nodeIterator().next();
                                    currentIterator = node.connections().iterator();
                                } else {
                                    return null;
                                }
                            } while (true);
                        }

                        private Iterator<Node<NodeDataType, WayDataType>> nodeIterator() {
                            if (nodeIterator == null) nodeIterator = nodes.iterator();
                            return nodeIterator;
                        }
                    };
                }

                @Override
                public Object[] toArray() {
                    Object[] a = new Object[size()];
                    int i = 0;
                    for (Node.Connection<NodeDataType, WayDataType> connection : this) {
                        a[i++] = connection;
                    }
                    return a;
                }

                public <T> T[] toArray(T[] a) {
                    // Estimate size of array; be prepared to see more or fewer elements
                    int size = size();
                    T[] r = a.length >= size ? a : (T[]) java.lang.reflect.Array.newInstance(a
                            .getClass()
                            .getComponentType(), size);
                    Iterator<Node.Connection<NodeDataType, WayDataType>> it = iterator();

                    for (int i = 0; i < r.length; i++) {
                        if (!it.hasNext()) { // fewer elements than expected
                            if (a == r) {
                                r[i] = null; // null-terminate
                            } else if (a.length < i) {
                                return Arrays.copyOf(r, i);
                            } else {
                                System.arraycopy(r, 0, a, 0, i);
                                if (a.length > i) {
                                    a[i] = null;
                                }
                            }
                            return a;
                        }
                        r[i] = (T) it.next();
                    }
                    // more elements than expected
                    return it.hasNext() ? finishToArray(r, it) : r;
                }

                private <T> T[] finishToArray(T[] r, Iterator<?> it) {
                    int len = r.length;
                    int i = len;
                    while (it.hasNext()) {
                        if (i == len) {
                            len = len + (len >> 1) + 1;
                            r = Arrays.copyOf(r, len);
                        }
                        r[i++] = (T) it.next();
                    }
                    // trim if over-allocated
                    return (i == len) ? r : Arrays.copyOf(r, i);
                }

                @Override
                public boolean add(Node.Connection<NodeDataType, WayDataType> connection) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean remove(Object o) {
                    Iterator<Node.Connection<NodeDataType, WayDataType>> it = iterator();
                    while (it.hasNext()) {
                        Node.Connection<NodeDataType, WayDataType> connection = it.next();
                        if (connection.equals(o)) {
                            it.remove();
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public boolean containsAll(Collection<?> c) {
                    for (Object o : c) {
                        if (!contains(o)) return false;
                    }
                    return true;
                }

                @Override
                public boolean addAll(Collection<? extends Node.Connection<NodeDataType, WayDataType>> c) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean removeAll(Collection<?> c) {
                    Iterator<Node.Connection<NodeDataType, WayDataType>> it = iterator();
                    boolean changed = false;
                    while (it.hasNext()) {
                        Node.Connection<NodeDataType, WayDataType> connection = it.next();
                        if (c.contains(connection)) {
                            it.remove();
                            changed = true;
                        }
                    }
                    return changed;
                }

                @Override
                public boolean retainAll(Collection<?> c) {
                    Iterator<Node.Connection<NodeDataType, WayDataType>> it = iterator();
                    boolean changed = false;
                    while (it.hasNext()) {
                        Node.Connection<NodeDataType, WayDataType> connection = it.next();
                        if (!c.contains(connection)) {
                            it.remove();
                            changed = true;
                        }
                    }
                    return changed;
                }

                @Override
                public void clear() {
                    Iterator<Node.Connection<NodeDataType, WayDataType>> it = iterator();
                    while (it.hasNext()) it.remove();
                }
            };
        }

        @Override
        public Node<NodeDataType, WayDataType> newNode(NodeDataType data) {
            LinkedNode node = new LinkedNode(data);
            nodes.add(node);
            return node;
        }

        @Override
        public void removeNode(Node<NodeDataType, WayDataType> node) {
            if (!LinkedNode.class.isInstance(node)) {
                throw new IllegalArgumentException("Not a valid node");
            }
            LinkedNode snode = (LinkedNode) node;
            if (!snode.graph().equals(this)) {
                throw new IllegalArgumentException("Node is not in this graph");
            }
            while (!snode.connections.isEmpty()) {
                snode.connections.get(0).remove();
            }
            while (!snode.origins.isEmpty()) {
                snode.origins.get(0).remove();
            }
            nodes.remove(snode);
        }

        @Override
        public Node.Connection<NodeDataType, WayDataType> newConnection(Node<NodeDataType, WayDataType> from, Node<NodeDataType, WayDataType> to, WayDataType way) {
            return from.newConnection(to, way);
        }

        @Override
        public void removeConnection(Node.Connection<NodeDataType, WayDataType> connection) {
            connection.from().removeConnection(connection);
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append("Graph: ");
            for (Node<NodeDataType, WayDataType> n : nodes) {
                b.append('\n').append(" - ").append(n);
            }
            return b.toString();
        }

        private class LinkedNode implements Node<NodeDataType, WayDataType> {
            private final NodeDataType data;
            private final ArrayList<Connection<NodeDataType, WayDataType>> connections;
            private final List<Connection<NodeDataType, WayDataType>> connectionsUnmodifiable;
            private final ArrayList<Connection<NodeDataType, WayDataType>> origins;

            public LinkedNode(NodeDataType data) {
                this.data = data;
                this.connections = new ArrayList<>();
                this.connectionsUnmodifiable = Collections.unmodifiableList(connections);
                this.origins = new ArrayList<>();
            }

            public LinkedGraph<NodeDataType, WayDataType> graph() {
                return LinkedGraph.this;
            }

            @Override
            public NodeDataType data() {
                return data;
            }

            @Override
            public List<Connection<NodeDataType, WayDataType>> connections() {
                return connectionsUnmodifiable;
            }

            @Override
            public Collection<Node<NodeDataType, WayDataType>> reachableNodes() {
                Set<Node<NodeDataType, WayDataType>> nodes = new HashSet<>();
                Deque<Node<NodeDataType, WayDataType>> unchecked = new ArrayDeque<>(Collections.singleton(this));
                Node<NodeDataType, WayDataType> node;
                while ((node = unchecked.pollFirst()) != null) {
                    if (!nodes.contains(node)) {
                        nodes.add(node);
                        for (Connection<NodeDataType, WayDataType> connection : node.connections()) {
                            unchecked.add(connection.to());
                        }
                    }
                }
                return Collections.unmodifiableCollection(nodes);
            }

            @Override
            public Connection<NodeDataType, WayDataType> newConnection(Node<NodeDataType, WayDataType> to, WayDataType way) {
                if (!to.getClass().equals(LinkedNode.class)) {
                    throw new IllegalArgumentException("Illegal node");
                }
                LinkedNode sto = (LinkedNode) to;
                if (sto.graph() != graph()) {
                    throw new IllegalArgumentException("Illegal node");
                }
                LinkedConnection con = new LinkedConnection(this, sto, way);
                sto.origins.add(con);
                connections.add(con);
                return con;
            }

            @Override
            public void removeConnection(Connection<NodeDataType, WayDataType> connection) {
                if (!LinkedConnection.class.isInstance(connection)) {
                    throw new IllegalArgumentException("This node does not have that connection!");
                }
                LinkedConnection linkedConnection = (LinkedConnection) connection;
                if (!connections.contains(linkedConnection)) {
                    throw new IllegalStateException("This node does not have that connection!");
                }
                linkedConnection.to.origins.remove(linkedConnection);
                linkedConnection.from.connections.remove(linkedConnection);
            }

            @Override
            public void remove() {
                removeNode(this);
            }

            private String ctoString() {
                Iterator<Connection<NodeDataType, WayDataType>> it = connections.iterator();
                if (!it.hasNext()) return "[]";
                StringBuilder sb = new StringBuilder();
                sb.append('[');
                for (; ; ) {
                    LinkedConnection e = (LinkedConnection) it.next();
                    sb.append(e.nodeToString());
                    if (!it.hasNext()) return sb.append(']').toString();
                    sb.append(',').append(' ');
                }
            }

            @Override
            public String toString() {
                return "Node{data=" + data + ", connections=" + ctoString() + '}';
            }
        }

        private class LinkedConnection implements Node.Connection<NodeDataType, WayDataType> {

            private final LinkedNode from;
            private final LinkedNode to;
            private final WayDataType way;

            public LinkedConnection(LinkedNode from, LinkedNode to, WayDataType way) {
                this.from = from;
                this.to = to;
                this.way = way;
            }

            @Override
            public Node<NodeDataType, WayDataType> to() {
                return to;
            }

            @Override
            public WayDataType way() {
                return way;
            }

            @Override
            public Node<NodeDataType, WayDataType> from() {
                return from;
            }

            @Override
            public Graph<NodeDataType, WayDataType> graph() {
                return LinkedGraph.this;
            }

            @Override
            public void remove() {
                removeConnection(this);
            }

            public String nodeToString() {
                return "Connection{to=" + to.data() + ", way=" + way + '}';
            }

            @Override
            public String toString() {
                return "Connection{from=" + from.data() + ", to=" + to.data() + ", way=" + way + '}';
            }
        }
    }


    class ArrayGraph<NodeDataType, WayDataType> implements Graph<NodeDataType, WayDataType> {
        private final ArrayList<NodeDataType> nodes = new ArrayList<>();
        private ArrayList<WayDataType>[][] connections;

        public ArrayGraph() {
            this.connections = new ArrayList[1][1];
        }

        @Override
        public Collection<Node<NodeDataType, WayDataType>> nodes() {
            Collection<Node<NodeDataType, WayDataType>> nodes = new HashSet<>();
            int i = 0;
            for (int node = 0; node < this.nodes.size(); node++) {
                nodes.add(new ArrayNode(node));
            }
            return Collections.unmodifiableCollection(nodes);
        }

        @Override
        public Collection<Node.Connection<NodeDataType, WayDataType>> connections() {
            List<Node.Connection<NodeDataType, WayDataType>> connections = new ArrayList<>();
            int max = Math.min(nodes.size(), this.connections.length);
            for (int fromId = 0; fromId < max; fromId++) {
                for (int toId = 0; toId < max; toId++) {
                    if (this.connections[fromId][toId] != null) {
                        for (int way = 0; way < this.connections[fromId][toId].size(); way++) {
                            connections.add(new ArrayConnection(new ArrayNode(fromId), new ArrayNode(toId), way, this.connections[fromId][toId].get(way)));
                        }
                    }
                }
            }
            return connections;
        }

        @Override
        public Node<NodeDataType, WayDataType> newNode(NodeDataType data) {
            int index = nodes.size();
            nodes.add(data);
            return new ArrayNode(index);
        }

        @Override
        public void removeNode(Node<NodeDataType, WayDataType> node) {
            ArrayNode an = cast(node);
            int index = an.node();
            if (index == -1) return;
            int newSize = nodes.size();
            if (newSize * 2 < connections.length && newSize != 0) {
                ArrayList<WayDataType>[][] oldConnections = connections;
                System.out.println("resizing to " + oldConnections.length / 2);
                connections = new ArrayList[oldConnections.length / 2][oldConnections.length / 2];
                for (int fromId = 0, oldFromId = 0; fromId < connections.length; fromId++, oldFromId++) {
                    if (oldFromId == index) oldFromId++;
                    for (int toId = 0, oldToId = 0; toId < connections[fromId].length; toId++, oldToId++) {
                        if (oldToId == index) oldToId++;
                        connections[fromId][toId] = oldConnections[oldFromId][oldToId];
                    }
                }
            }
        }

        @Override
        public Node.Connection<NodeDataType, WayDataType> newConnection(Node<NodeDataType, WayDataType> from, Node<NodeDataType, WayDataType> to, WayDataType way) {
            ArrayNode afrom = cast(from);
            ArrayNode ato = cast(to);
            int min = Math.max(afrom.node(), ato.node());
            if (connections.length <= min) {
                int newLength = connections.length;
                do {
                    newLength *= 2;
                } while (newLength <= min);
                System.out.println("resizing to " + newLength + "x" + newLength);
                ArrayList<WayDataType>[][] oldConnections = connections;
                connections = new ArrayList[newLength][newLength];
                for (int fromId = 0; fromId < oldConnections.length; fromId++) {
                    System.arraycopy(oldConnections[fromId], 0, connections[fromId], 0, oldConnections[fromId].length);
                }
            }
            ArrayList<WayDataType> ways = connections[afrom.node][ato.node];
            if (ways == null) {
                ways = new ArrayList<>(1);
                connections[afrom.node][ato.node] = ways;
            }
            int wayId = ways.size();
            ways.add(wayId, way);
            return new ArrayConnection(afrom, ato, wayId, way);
        }

        @Override
        public void removeConnection(Node.Connection<NodeDataType, WayDataType> connection) {
            ArrayConnection con = cast(connection);
            WayDataType way = con.way();
            if (way == null) return;
            ArrayList<WayDataType> ways = connections[con.from.node()][con.to.node()];
            ways.remove(con.wayId);
            if (ways.isEmpty()) {
                connections[con.from.node][con.to.node] = null;
            }
        }

        private ArrayConnection cast(Node.Connection<NodeDataType, WayDataType> connection) {
            if (!ArrayConnection.class.isInstance(connection)) throw new IllegalArgumentException("Wrong connection");
            if (connection.graph() != ArrayGraph.this) throw new IllegalStateException("Connection not of this graph");
            return (ArrayConnection) connection;
        }

        private ArrayNode cast(Node<NodeDataType, WayDataType> node) {
            if (!ArrayNode.class.isInstance(node)) throw new IllegalArgumentException("Wrong node");
            if (((ArrayNode) node).graph() != this) throw new IllegalStateException("Node not of this graph");
            return (ArrayNode) node;
        }

        private class ArrayNode implements Node<NodeDataType, WayDataType> {

            private int node;
            private NodeDataType data;

            public ArrayNode(int node) {
                this.node = node;
                this.data = nodes.get(node);
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                ArrayNode arrayNode = (ArrayNode) o;
                return node == arrayNode.node && data.equals(arrayNode.data);
            }

            @Override
            public int hashCode() {
                return Objects.hash(node, data);
            }

            public int node() {
                if (node == -1) return -1;
                if (nodes.size() <= node) {
                    data = null;
                    return node = -1;
                }
                NodeDataType current = nodes.get(node);
                if (!current.equals(data)) {
                    node = nodes.indexOf(data);
                    if (node == -1) {
                        data = null; // Node removed
                    }
                }
                return node;
            }

            @Override
            public List<Connection<NodeDataType, WayDataType>> connections() {
                List<Connection<NodeDataType, WayDataType>> con = new ArrayList<>();
                int node = node();
                if (node == -1) {
                    return Collections.emptyList(); // Node removed
                }
                ArrayList<WayDataType>[] con2 = connections[node];
                for (int toNr = 0; toNr < con2.length; toNr++) {
                    if (con2[toNr] == null) continue;
                    for (int i = 0; i < con2[toNr].size(); i++) {
                        con.add(new ArrayConnection(new ArrayNode(node), new ArrayNode(toNr), i, con2[toNr].get(i)));
                    }
                }
                return Collections.unmodifiableList(con);
            }

            @Override
            public Connection<NodeDataType, WayDataType> newConnection(Node<NodeDataType, WayDataType> to, WayDataType way) {
                return graph().newConnection(this, to, way);
            }

            @Override
            public void removeConnection(Connection<NodeDataType, WayDataType> connection) {
                graph().removeConnection(connection);
            }

            @Override
            public NodeDataType data() {
                node();
                return data;
            }

            @Override
            public ArrayGraph<NodeDataType, WayDataType> graph() {
                return ArrayGraph.this;
            }

            @Override
            public void remove() {
                graph().removeNode(this);
            }

            @Override
            public Collection<Node<NodeDataType, WayDataType>> reachableNodes() {
                Collection<Node<NodeDataType, WayDataType>> reachable = new HashSet<>();
                Set<Integer> used = new HashSet<>();
                reachableNodes(reachable, used, this.node());
                used.clear();
                return Collections.unmodifiableCollection(reachable);
            }

            private void reachableNodes(Collection<Node<NodeDataType, WayDataType>> nodes, Set<Integer> used, int currentNode) {
                used.add(currentNode);
                nodes.add(new ArrayNode(currentNode));
                for (int toNr = 0; toNr < connections[currentNode].length; toNr++) {
                    if (used.contains(toNr)) continue;
                    ArrayList<WayDataType> ways = connections[currentNode][toNr];
                    if (ways == null) continue;
                    reachableNodes(nodes, used, toNr);
                }
            }
        }


        private class ArrayConnection implements Node.Connection<NodeDataType, WayDataType> {

            private final ArrayNode from;
            private final ArrayNode to;
            private int wayId;
            private WayDataType way;

            public ArrayConnection(ArrayNode from, ArrayNode to, int wayId, WayDataType way) {
                this.from = from;
                this.to = to;
                this.wayId = wayId;
                this.way = way;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                ArrayConnection that = (ArrayConnection) o;
                return wayId == that.wayId && Objects.equals(from, that.from) && Objects.equals(to, that.to) && Objects.equals(way, that.way);
            }

            @Override
            public int hashCode() {
                return Objects.hash(from, to, wayId, way);
            }

            @Override
            public ArrayNode from() {
                return from;
            }

            @Override
            public ArrayNode to() {
                return to;
            }

            @Override
            public ArrayGraph<NodeDataType, WayDataType> graph() {
                return ArrayGraph.this;
            }

            @Override
            public WayDataType way() {
                if (way == null) return null;
                ArrayList<WayDataType> cons = connections[from.node()][to.node()];
                if (cons.size() <= wayId) {
                    wayId = -1;
                    return way = null;
                }
                WayDataType current = cons.get(wayId);
                if (!current.equals(way)) {
                    wayId = cons.indexOf(way);
                    if (wayId == -1) {
                        return way = null;
                    }
                }
                return way;
            }

            @Override
            public void remove() {
                graph().removeConnection(this);
            }
        }
    }


    class Util {
        private static final class SimplePath<NodeDataType, WayDataType> implements Path<NodeDataType, WayDataType> {
            private final ArrayList<Node.Connection<NodeDataType, WayDataType>> connections = new ArrayList<>();
            private final Graph<NodeDataType, WayDataType> graph;

            public SimplePath(Graph<NodeDataType, WayDataType> graph) {
                this.graph = graph;
            }

            public SimplePath(Graph<NodeDataType, WayDataType> graph, Collection<Node.Connection<NodeDataType, WayDataType>> connections) {
                this(graph);
                this.connections.addAll(connections);
            }

            @Override
            public Iterator<Node.Connection<NodeDataType, WayDataType>> iterator() {
                return connections.iterator();
            }

            @Override
            public SimplePath<NodeDataType, WayDataType> clone() {
                return new SimplePath<>(graph, new ArrayList<>(connections));
            }

            @Override
            public Graph<NodeDataType, WayDataType> graph() {
                return graph;
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append("Path[");
                boolean first = true;
                for (Node.Connection<NodeDataType, WayDataType> connection : connections) {
                    if (first) {
                        first = false;
                        sb.append(connection.from().data().toString());
                    }
                    sb.append(" -> ").append(connection.way()).append(" -> ").append(connection.to().data().toString());
                }
                sb.append(']');
                return sb.toString();
            }

            @Override
            public Node<NodeDataType, WayDataType> last() {
                return connections.isEmpty() ? null : connections.get(connections.size() - 1).to();
            }

            @Override
            public Node<NodeDataType, WayDataType> first() {
                return connections.isEmpty() ? null : connections.get(0).from();
            }
        }
    }
}
