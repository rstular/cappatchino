package cappatchino.patcher.utils

import java.lang.RuntimeException

internal class Graph<T>() {
    private val nodes = mutableMapOf<T, Node<T>>()

    class Node<T>(val nodeValue: T) {
        val edges = mutableSetOf<Node<T>>()

        fun addEdge(node: Node<T>): Boolean {
            return edges.add(node)
        }
    }

    constructor(initialNodes: Collection<T>) : this() {
        initialNodes.forEach { addNode(it) }
    }

    private fun addNode(nodeValue: T) {
        nodes[nodeValue] = Node(nodeValue)
    }

    fun addEdge(from: T, to: T): Boolean {
        val fromNode = nodes[from] ?: throw IllegalArgumentException("Node $from does not exist")
        val toNode = nodes[to] ?: throw IllegalArgumentException("Node $to does not exist")
        return fromNode.addEdge(toNode)
    }

    fun topologicalOrder(): List<T>? {
        val inDegrees = mutableMapOf<Node<T>, Int>()
        for (node in nodes.values) {
            for (edge in node.edges) {
                inDegrees[edge] = (inDegrees[edge] ?: 0) + 1
            }
        }

        val queue = nodes.values.filter { it !in inDegrees }.toCollection(ArrayDeque())
        val result = ArrayList<T>(nodes.size)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            result.add(node.nodeValue)
            for (edge in node.edges) {
                inDegrees[edge] = (inDegrees[edge] ?: throw RuntimeException("Illegal graph state")) - 1
                if (inDegrees[edge] == 0) {
                    queue.addLast(edge)
                }
            }
        }

        if (result.size != nodes.size) return null
        return result
    }
}