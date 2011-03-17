/*
 * CampusWorldWithGUI
 *
 * $Id: CampusWorldWithUI.java,v 1.2 2010-08-25 20:46:49 mcoletti Exp $
 */

package sim.app.geo.campusworld;

import com.vividsolutions.jts.io.ParseException;
import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.simple.OvalPortrayal2D;


/** MASON GUI wrapper for Campus World demo
 *
 */
public class CampusWorldWithUI extends GUIState
{

    private Display2D display;
    private JFrame displayFrame;

    private GeomVectorFieldPortrayal walkwaysPortrayal = new GeomVectorFieldPortrayal(true);
    private GeomVectorFieldPortrayal buildingPortrayal = new GeomVectorFieldPortrayal(true);
    private GeomVectorFieldPortrayal roadsPortrayal = new GeomVectorFieldPortrayal(true);
    private GeomVectorFieldPortrayal agentPortrayal = new GeomVectorFieldPortrayal();

    public CampusWorldWithUI(SimState state)
    {
        super(state);
    }

    public CampusWorldWithUI() throws ParseException
    {
        super(new CampusWorld(System.currentTimeMillis()));
    }

    public void init(Controller controller)
    {
        super.init(controller);

        display = new Display2D(CampusWorld.WIDTH, CampusWorld.HEIGHT, this, 1);

        display.attach(walkwaysPortrayal, "Walkways");
        display.attach(buildingPortrayal, "Buildings");
        display.attach(roadsPortrayal, "Roads");
        display.attach(agentPortrayal, "Agents");

        displayFrame = display.createFrame();
        controller.registerFrame(displayFrame);
        displayFrame.setVisible(true);
    }


    public void start()
    {
        super.start();
        setupPortrayals();
    }

    private void setupPortrayals()
    {
        CampusWorld world = (CampusWorld)state;

        walkwaysPortrayal.setField(world.walkways);
        walkwaysPortrayal.setPortrayalForAll(new GeomPortrayal(Color.CYAN,true));

        buildingPortrayal.setField(world.buildings);
        buildingPortrayal.setPortrayalForAll(new GeomPortrayal(Color.DARK_GRAY,true));

        roadsPortrayal.setField(world.roads);
        roadsPortrayal.setPortrayalForAll(new GeomPortrayal(Color.GRAY,true));

        agentPortrayal.setField(world.agents);
        agentPortrayal.setPortrayalForAll(new GeomPortrayal(Color.RED,10.0,true));
        //agentPortrayal.setPortrayalForAll(new OvalPortrayal2D(Color.RED,6.0));
        
        display.reset();
        display.setBackdrop(Color.WHITE);

        display.repaint();
    }

    public static void main(String[] args)
    {
        CampusWorldWithUI worldGUI = null;

        try
        {
            worldGUI = new CampusWorldWithUI();
        }
        catch (ParseException ex)
        {
            Logger.getLogger(CampusWorldWithUI.class.getName()).log(Level.SEVERE, null, ex);
        }

        Console console = new Console(worldGUI);
        console.setVisible(true);
    }

}
