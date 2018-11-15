import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.DefaultGraph;
import org.graphstream.stream.file.FileSource;
import org.graphstream.stream.file.FileSourceGML;

public class PageRank {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		if (args.length != 3) {
			System.out.println(
					"Incorrect number of arguments. Please provide args (GML path, n - number of PR iterations, d - dampening factor (0<d<1))");
			return;
		}

		int NUM_ITERATIONS = Integer.parseInt(args[1]);
		double SCALE_FACTOR = Double.parseDouble(args[2]);

		if (NUM_ITERATIONS <= 0) {
			System.out.println("Number of iterations cannot be less than or equal to 0");
			return;
		}
		if (SCALE_FACTOR <= 0 || SCALE_FACTOR >= 1) {
			System.out.println("Scale down factor must be between 0 and 1 non-inclusive");
			return;
		}

		Graph g = new DefaultGraph("graph");

		FileSource fs = new FileSourceGML();
		fs.addSink(g);

		try {
			fs.readAll(new FileInputStream(args[0]));
		} catch (FileNotFoundException e) {
			System.out.println("GML path is invalid. Please correct your path");
			e.printStackTrace();
		} catch (IOException e) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(new File(args[0])));
				// take out starting line in case it has "creator"
				br.readLine();
				fs.readAll(br);
				br.close();
			} catch (FileNotFoundException e1) {
				System.out.println("GML path is invalid. Please correct your path");
				e1.printStackTrace();
			} catch (IOException e1) {
				System.out.println(
						"Incompatible GML file, we tried parsing first author line out and running like normal...");
				e1.printStackTrace();
			}
		}

		Collection<Node> nodes = g.getNodeSet();

		for (Node n : nodes) {
			n.setAttribute("rank", 1 / (double) nodes.size());
			n.setAttribute("newRank", 0);
			n.setAttribute("iterations", new ArrayList<Double>());
		}

		int iterations = 0;
		while (iterations < NUM_ITERATIONS) {
			for (Node n : nodes) {
				double sum = 0;

				for (Edge e : n.getEnteringEdgeSet()) {
					Node source = e.getSourceNode();
					sum += source.getNumber("rank") / source.getOutDegree();
				}

				n.setAttribute("newRank", (SCALE_FACTOR * sum) + ((1 - SCALE_FACTOR) / (double) nodes.size()));
			}

			for (Node n : nodes) {
				double newRank = n.getNumber("newRank");
				((ArrayList<Double>) n.getAttribute("iterations")).add(newRank);
				n.setAttribute("rank", n.getNumber("newRank"));
				n.setAttribute("newRank", 0);
			}
			iterations++;
		}

		TreeSet<Node> sortedNodes = new TreeSet<Node>(new NodeComparator());
		sortedNodes.addAll(nodes);

		Iterator<Node> nodeIt = sortedNodes.descendingIterator();
		while (nodeIt.hasNext()) {
			Node n = nodeIt.next();

			System.out.printf("Node %-8s:", n.getId());
			for (Double d : ((ArrayList<Double>) n.getAttribute("iterations"))) {
				System.out.printf("  %.8f", d);
			}
			System.out.println();
		}
	}

	public static class NodeComparator implements Comparator<Node> {
		@Override
		public int compare(Node o1, Node o2) {
			return ((Double) o1.getNumber("rank")).compareTo(o2.getNumber("rank"));
		}
	}

}
