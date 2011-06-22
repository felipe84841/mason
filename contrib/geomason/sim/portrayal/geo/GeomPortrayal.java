/* 
Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
George Mason University Mason University Licensed under the Academic
Free License version 3.0

See the file "LICENSE" for more information
*/
package sim.portrayal.geo;

// we can't do a mass import of java.awt.* since java.awt.Polygon and
// com.vividsolutions.jts.geom.Polygon will conflict
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.geom.util.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.*;
import java.util.ArrayList;

import sim.portrayal.*;
import sim.display.*;
import sim.field.geo.GeomVectorField;
import sim.util.geo.*;
import sim.portrayal.inspector.*;

/**
 * The portrayal for MasonGeometry objects. The class draws the JTS geometry
 * object (currently, we can draw Point, LineString, Polygon, MultiLineString
 * and MultiPolygon objects), and sets up the inspectors for the MasonGeometry
 * object. The inspector is TabbedInspector with at most three tabs: the first
 * tab shows various information about the JTS geometry, the second tab shows
 * the associated attribute information, and the third tab shows information
 * about the MasonGeometry userData field, which can be any Java object.
 */
public class GeomPortrayal extends SimplePortrayal2D
{

	private static final long serialVersionUID = 472960663330467429L;

	/** How to paint each object */
	public Paint paint;

	/** Scale for each object */
	public double scale;

	/** Should objects be filled when painting? */
	public boolean filled;

	/** Default constructor creates filled, gray objects with a scale of 1.0 */
	public GeomPortrayal() {
		this(Color.GRAY, 1.0, true);
	}

	public GeomPortrayal(Paint paint) {
		this(paint, 1.0, true);
	}

	public GeomPortrayal(double scale) {
		this(Color.GRAY, scale, true);
	}

	public GeomPortrayal(Paint paint, double scale) {
		this(paint, scale, true);
	}

	public GeomPortrayal(Paint paint, boolean filled) {
		this(paint, 1.0, filled);
	}

	public GeomPortrayal(double scale, boolean filled) {
		this(Color.GRAY, scale, filled);
	}

	public GeomPortrayal(boolean filled) {
		this(Color.GRAY, 1.0, filled);
	}

	public GeomPortrayal(Paint paint, double scale, boolean filled) {
		this.paint = paint;
		this.scale = scale;
		this.filled = filled;
	}

	/**
	 * Use our custom Inspector. We create a TabbedInspector for each object
	 * that allows inspection of the JTS geometry, attribute information, and
	 * the MasonGeometry userData field.
	 */
	public Inspector getInspector(LocationWrapper wrapper, GUIState state)
	{
		if (wrapper == null)
			return null;
		TabbedInspector inspector = new TabbedInspector();

		// for basic geometry information such as area, perimeter, etc.
		inspector.addInspector(new SimpleInspector(wrapper.getObject(), state, null), "Geometry");

		Object o = wrapper.getObject();
		if (o instanceof MasonGeometry)
		{
			MasonGeometry gw = (MasonGeometry) o;

			if (gw.geometry.getUserData() instanceof ArrayList<?>)
			{
				@SuppressWarnings("unchecked")
				ArrayList<AttributeField> aList = (ArrayList<AttributeField>) gw.geometry.getUserData();

				boolean showAttrs = false;
				for (int i = 0; i < aList.size(); i++)
				{
					if (!aList.get(i).hidden)
					{
						showAttrs = true;
						break;
					}
				}

				if (showAttrs)
				{ // only add attributes tag if JTS geometry has attributes
					GeometryProperties properties = new GeometryProperties(aList);
					inspector.addInspector(new SimpleInspector(properties, state, null), "Attributes");
				}

				if (gw.userData != null) // only add userData inspector if there
					// is actually userdata
					inspector.addInspector(new SimpleInspector(gw.userData, state, null), "User Data");
			}
		}
		return inspector;
	}

	// AffineTransform transform = new AffineTransform();
	// com.vividsolutions.jts.geom.util.AffineTransformation jtsTransform;

	/**
	 * Draw a JTS geometry object. The JTS geometries are converted to Java
	 * general path objects, which are then drawn using the native Graphics2D
	 * methods.
	 */
	public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
	{
		GeomInfo2D gInfo;
		if (info instanceof GeomInfo2D)
			gInfo = (GeomInfo2D) info;
		else
			gInfo = new GeomInfo2D(info, new AffineTransform());

		MasonGeometry gm = (MasonGeometry) object;
		Geometry geometry = gm.getGeometry();

		if (geometry.isEmpty())
			return;

		if (paint != null)
			graphics.setPaint(paint);

		// don't have cached shape or the transform changed, so need to build
		// the shape
		if ((gm.isMovable) || (gm.shape == null) || (!gm.transform.equals(gInfo.transform)))
		{
			gm.transform.setTransform(gInfo.transform);
			if (geometry instanceof Point)
			{
				Point point = (Point) geometry;
				double offset = 3 * scale / 2.0; // used to center point
				Ellipse2D.Double ellipse = new Ellipse2D.Double(point.getX() - offset, point.getY() - offset,
						3 * scale, 3 * scale);

				GeneralPath path = (GeneralPath) (new GeneralPath(ellipse).createTransformedShape(gInfo.transform));
				gm.shape = path;
			}
			else if (geometry instanceof LineString)
			{
				gm.shape = drawGeometry(geometry, gInfo, false);
				filled = false;
			}
			else if (geometry instanceof Polygon)
				gm.shape = drawPolygon((Polygon) geometry, gInfo, filled);
			else if (geometry instanceof MultiLineString)
			{
				// draw each LineString individually
				MultiLineString multiLine = (MultiLineString) geometry;
				for (int i = 0; i < multiLine.getNumGeometries(); i++)
				{
					GeneralPath p = drawGeometry(multiLine.getGeometryN(i), gInfo, false);
					if (i == 0)
						gm.shape = p;
					else
						gm.shape.append(p, false);
				}
				filled = false;
			}
			else if (geometry instanceof MultiPolygon)
			{
				// draw each Polygon individually
				MultiPolygon multiPolygon = (MultiPolygon) geometry;
				for (int i = 0; i < multiPolygon.getNumGeometries(); i++)
				{
					GeneralPath p = drawPolygon((Polygon) multiPolygon.getGeometryN(i), gInfo, filled);
					if (i == 0)
						gm.shape = p;
					else
						gm.shape.append(p, false);
				}
			}
			else
				throw new UnsupportedOperationException("Unsupported JTS type for draw()" + geometry);
		}

		// now draw it!
		if (filled)
			graphics.fill(gm.shape);
		else
			graphics.draw(gm.shape);
		return;

	}

	/**
	 * Helper function for drawing a JTS polygon.
	 * 
	 * <p>
	 * Polygons have two sets of coordinates; one for the outer ring, and
	 * optionally another for internal ring coordinates. Draw the outer ring
	 * first, and then draw each internal ring, if they exist.
	 * */
	GeneralPath drawPolygon(Polygon polygon, GeomInfo2D info, boolean fill)
	{
		GeneralPath p = drawGeometry(polygon.getExteriorRing(), info, fill);

		for (int i = 0; i < polygon.getNumInteriorRing(); i++)
		{ // fill for internal rings will always be false as they are literally
			// "holes" in the polygon
			p.append(drawGeometry(polygon.getInteriorRingN(i), info, false), false);
		}
		return p;
	}

	/**
	 * Helper function to draw a JTS geometry object. The coordinates of the JTS
	 * geometry are converted to a native Java GeneralPath which is used to draw
	 * the object.
	 */
	GeneralPath drawGeometry(Geometry geom, GeomInfo2D info, boolean fill)
	{
		Coordinate coords[] = geom.getCoordinates();
		GeneralPath path = new GeneralPath(GeneralPath.WIND_NON_ZERO, coords.length);
		path.moveTo((float) coords[0].x, (float) coords[0].y);

		for (int i = 1; i < coords.length; i++)
			path.lineTo((float) coords[i].x, (float) coords[i].y);

		path.transform(info.transform);

		return path;
	}

	/** Determine if the object was hit or not. */
	public boolean hitObject(Object object, DrawInfo2D range)
	{
		double SLOP = 2.0;
		MasonGeometry geom = (MasonGeometry) object;
		
		if (geom.shape == null)		// if there's no shape, you can't hit it
			return false;

		return geom.shape.intersects(range.clip.x - SLOP / 2, range.clip.y - SLOP / 2, range.clip.width + SLOP / 2,
				range.clip.height + SLOP / 2);

	}
}
