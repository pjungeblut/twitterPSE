package gui.standardMap;

import gui.GUIController;
import gui.GUIElement.UpdateType;

import java.util.HashMap;

import javax.swing.JDialog;

import unfolding.MyUnfoldingMap;
import mysql.result.TweetsAndRetweets;

@SuppressWarnings("serial")
public class StandardMapDialog extends JDialog {
	private GUIController superController;
	private TweetsAndRetweets uneditedData;
	private MyUnfoldingMap map = new MyUnfoldingMap();
	private HashMap<String, Double> calculatedData;

	public StandardMapDialog(GUIController superController) {
		this.superController = superController;
		setSize(600, 400);
		setTitle("Map");
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		add(map);
		map.init();
	}

	@SuppressWarnings("incomplete-switch")
	public void update(UpdateType type) {
		switch (type) {
		case CLOSE:
			if (map != null) {
				map.exit();
			}
			break;
		case TWEET:
			uneditedData = superController.getDataByLocation();
			HashMap<String, Integer> forCalc = new HashMap<String, Integer>();
			for (mysql.result.Retweets r : uneditedData.retweets) {
				int counter = r.getCounter();
				String id = r.getLocationCode();
				forCalc.put(id, counter);
			}

			calculatedData = superController.getDisplayValuePerCountry(
					uneditedData, forCalc);

			// TODO: repaint pane
			// TODO: Get calculated data from somewhere
			// map.update(); insert new data
			break;
		case GUI_STARTED:
			map.redraw();
			setVisible(true);
//			repaint();
			break;
		}
	}

}
