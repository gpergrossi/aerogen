package dev.mortus.voronoi.internal;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import dev.mortus.util.data.Pair;
import dev.mortus.util.math.LineSeg;
import dev.mortus.util.math.Ray;
import dev.mortus.util.math.Rect;
import dev.mortus.util.math.Vec2;
import dev.mortus.voronoi.Edge;
import dev.mortus.voronoi.Site;
import dev.mortus.voronoi.Vertex;
import dev.mortus.voronoi.Voronoi;
import dev.mortus.voronoi.internal.tree.Arc;
import dev.mortus.voronoi.internal.tree.Breakpoint;
import dev.mortus.voronoi.internal.tree.ShoreTree;
import dev.mortus.voronoi.internal.tree.TreeNode;


/**
 * Listened to https://www.youtube.com/watch?v=NXXivAiS59Y while coding. Recommended.
 * 
 * @author Gregary
 *
 */
public final class BuildState {
	
	private static final Comparator<Event> EventOrder = new Comparator<Event>() {
		public int compare(Event o1, Event o2) {
			// Lowest Y value first
			double y1 = o1.getPosition().getY();
			double y2 = o2.getPosition().getY();
			if(y1 < y2) return -1;
			if(y1 > y2) return 1;

			// Lowest X value first
			double x1 = o1.getPosition().getX();
			double x2 = o2.getPosition().getX();
			if(x1 < x2) return -1;
			if(x1 > x2) return 1;
			
			return 0;
		}
	};

	private final Voronoi voronoi;
	private final Queue<Event> eventQueue;
	private final ShoreTree shoreTree;
	private double sweeplineY = Double.NaN;
	private Rectangle2D bounds;
	private int eventsProcessed;
	private boolean finished;
	private List<Edge> edges;
	private int numSites;

	public BuildState (Voronoi voronoi, Rectangle2D bounds) {
		this.voronoi = voronoi;
		this.eventQueue = new PriorityQueue<Event>(EventOrder);
		this.shoreTree = new ShoreTree();
		
		this.bounds = new Rectangle2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(),bounds.getHeight());
		this.edges = new ArrayList<Edge>();
		
		this.eventsProcessed = 0;
	}

	public void initSiteEvents(List<Site> sites) {
		eventQueue.clear();
		TreeNode.IDCounter = 0;
		Site.IDCounter = 0;
		edges.clear();
		finished = false;
		numSites = sites.size();
		
		for(Site site : sites) addEvent(Event.createSiteEvent(site));

		// Create tree with first site
		Event e = eventQueue.poll();
		shoreTree.initialize(e.site);
		eventsProcessed = 1;
	}
	
	public boolean hasNextEvent() {
		return !finished;
	}
	
	private void addEvent(Event e) {
		 if (e == null) return;
		 eventQueue.offer(e);
	}
	
	private void removeEvent(Event e) {
		 if (e == null) return;
		 eventQueue.remove(e);
	}
	
	public void processNextEvent() {
		if (!shoreTree.isInitialized()) throw new RuntimeException("Shore Tree has not yet been initialized");
		
		if (eventQueue.size() == 0) {
			finish();
			return;
		}
		
		Event e = eventQueue.poll();
		sweeplineY = e.getPosition().getY();
		
		switch(e.type) {
			case SITE:
				processSiteEvent(e.site);
				break;
				
			case CIRCLE:
				processCircleEvent(e.arc);
				break;
		}
		
		eventsProcessed++;
		
		if (Voronoi.DEBUG) {
			int siteCount = 0, circleCount = 0;
			for (Event event : eventQueue) {
				if (event.type == Event.Type.SITE) siteCount++;
				if (event.type == Event.Type.CIRCLE) circleCount++;
			}
			System.out.println("processed: "+eventsProcessed+" events. "+siteCount+" site events and "+circleCount+" circle events remaining.");
			System.out.println("just processed: "+e+", next: "+eventQueue.peek());
			
			TreeNode n = shoreTree.getRoot().getFirstDescendant();
			System.out.println(" ========== TREELIST ==========");
			while (n != null) {
				System.out.println(n);
				n = n.getSuccessor();
			}
		}
		
	}

	private void processSiteEvent(Site site) {
		Arc arcUnderSite = shoreTree.getArcUnderSite(sweeplineY, site);
		removeEvent(arcUnderSite.getCircleEvent());
		
		// Split the arc with two breakpoints (possibly just one) and insert an arc for the site event
		Arc newArc = arcUnderSite.insertArc(this, site);
		
		// Check for circle events on neighboring arcs
		for (Arc neighbor : newArc.getNeighborArcs()) {
			addEvent(neighbor.checkCircleEvent(this));
		}
		
		// Create edges
		Vertex sharedVertex = null;
		for (Breakpoint bp : newArc.getBreakpoints() ) {
			Edge newEdge = bp.checkNewEdge(sweeplineY, bounds, sharedVertex);
			if (newEdge != null) sharedVertex = newEdge.start();
		}
	}
	
	private void processCircleEvent(Arc arc) {
		// Save these, they will change
		Pair<Arc> neighbors = arc.getNeighborArcs(); 
		Breakpoint predecessor = arc.getPredecessor();
		Breakpoint successor = arc.getSuccessor();
		
		// Step 1. Finish the edges of each breakpoint
		Vertex sharedVertex = new Vertex(predecessor.getPosition(sweeplineY));
		for (Breakpoint bp : arc.getBreakpoints()) {
			bp.edge.end(sharedVertex);
			addEdge(bp.edge);
			bp.edge = null;
		}
		
		// Step 2. Remove arc and one of its breakpoints
		TreeNode parentBreakpoint = arc.getParent();
		TreeNode sibling = arc.getSibling();
		if (parentBreakpoint != successor && parentBreakpoint != predecessor) {
			if (arc.isLeftChild()) throw new RuntimeException("Unexpected successor! "+successor + ", should be "+parentBreakpoint);
			if (arc.isRightChild()) throw new RuntimeException("Unexpected predecessor! "+predecessor + ", should be "+parentBreakpoint);
			throw new RuntimeException("The parent of any arc should be its successor or its predecessor!");
		}
		sibling.disconnect();
		parentBreakpoint.replaceWith(sibling);
		
		// Step 3. Update the remaining breakpoint
		Breakpoint remainingBP = null;
		if (parentBreakpoint == successor) remainingBP = predecessor;
		if (parentBreakpoint == predecessor) remainingBP = successor;
		remainingBP.updateArcs();
		
		// Step 4. Update circle events
		for (Arc neighbor : neighbors) {
			removeEvent(neighbor.circleEvent);
			addEvent(neighbor.checkCircleEvent(this));
		}
		
		// Step 5. Form new edge
		remainingBP.checkNewEdge(sweeplineY, bounds, sharedVertex);
	}
	
	private void finish() {
		if (sweeplineY < bounds.getMinY()) sweeplineY = bounds.getMaxY();
		// TODO merge vertices, clip edges, create new edges along boundaries, link edges/vertices/sites
		// report completed graph back to voronoi

		Rect boundsRect = new Rect(bounds);
		
		// Extend unfinished edges
		TreeNode node = shoreTree.getRoot().getFirstDescendant();
		for (; node != null; node = node.getSuccessor()) {
			if (!(node instanceof Breakpoint)) continue;
			Breakpoint bp = (Breakpoint) node;
			
			Edge edge = bp.edge;
			Vec2 endPoint = bp.getPosition(sweeplineY);
			if (bounds.contains(endPoint.toPoint2D())) {
				Ray edgeRay = new Ray(endPoint, bp.getDirection());
				endPoint = boundsRect.intersect(edgeRay).get(0);
			}
			
			edge.end(new Vertex(endPoint));
			addEdge(edge);
		}
		
		// Clip all edges
		List<Edge> remove = new ArrayList<Edge>();
		for (Edge edge : edges) {
			LineSeg seg = edge.toLineSeg();
			seg = boundsRect.clip(seg);
			
			if (seg == null) {
				remove.add(edge);
				continue;
			}
			
			boolean sameStart = seg.pos.equals(edge.start().getPosition());
			boolean sameEnd = seg.end.equals(edge.end().getPosition());
			
			if (!sameStart || !sameEnd) {				
				Vertex start = edge.start();
				Vertex end = edge.end();
				if (!sameStart) start = new Vertex(seg.pos);
				if (!sameEnd) end = new Vertex(seg.end);
				
				edge.start(start);
				edge.end(end);
			}
		}
		for (Edge edge : remove) removeEdge(edge);
		
		finished = true;
		System.out.println("finished");
	}
	
	private void removeEdge(Edge edge) {
		this.edges.remove(edge);
	}

	public boolean isFinished() {
		return finished;
	}
	
	private void addEdge(Edge edge) {
		this.edges.add(edge);
	}

	public List<Edge> getEdges() {
		return Collections.unmodifiableList(edges);
	}
	
	public double getSweeplineY() {
		return sweeplineY;
	}
	
	public Rectangle2D getBounds() {
		return new Rectangle2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
	}

	public void drawDebugState(Graphics2D g) {
		g.setFont(new Font("Consolas", Font.PLAIN, 12));
		this.shoreTree.draw(this, g);
		
		g.setColor(Color.YELLOW);
		Line2D line = new Line2D.Double(bounds.getMinX(), sweeplineY, bounds.getMaxX(), sweeplineY);
		g.draw(line);
		
		AffineTransform transform = g.getTransform();
		AffineTransform identity = new AffineTransform();
		
		for (Site s : voronoi.getSites()) {
			Ellipse2D sitedot = new Ellipse2D.Double(s.getX()-1, s.getY()-1, 2, 2);
			g.setColor(new Color(0,128,0));
			g.draw(sitedot);
			
			g.setTransform(identity);
			Point2D pt = new Point2D.Double(s.getX(), s.getY());
			transform.transform(pt, pt);
			g.setColor(new Color(0,255,0));
			g.drawString(""+s.id, (int) pt.getX(), (int) pt.getY());
			g.setTransform(transform);
		}
	}

	public void debugAdvanceSweepline(double v) {
		this.sweeplineY += v;
	}

	public int getEventsProcessed() {
		return eventsProcessed;
	}

	public int getTheoreticalMaxSteps() {
		int maxPossibleCircleEvents = numSites*2 - 5;
		if (numSites <= 2) maxPossibleCircleEvents = 0;
		return numSites + maxPossibleCircleEvents;
	}

	public void processNextEventVerbose() {
		boolean hold = Voronoi.DEBUG;
		Voronoi.DEBUG = true;
		processNextEvent();
		Voronoi.DEBUG = hold;
	}
	
}
