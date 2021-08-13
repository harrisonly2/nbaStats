import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.*;

import java.net.URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;

public class nbaStats {
	
	// boolean to indicate if the preloading of the graph for 
	// mvp-bfs functionality
	boolean preloadDone = false;
	// class for the thread of the preloead that implements runnable 
	class BFSPreload implements Runnable {
        private Thread t;
		private String threadName;
		// constructor of teh preload class 
		BFSPreload( String name) {
		    threadName = name;
		    System.out.println("Creating " +  threadName );
		}
		// implementation of the run method 
		// it will attempt to open the seralized graph
		// otherwise it will build the graph from scratch using the 
		// buildGraph method 
		public void run() {
			try {
				FileInputStream fis = new FileInputStream("playerGraph.ser");
			    ObjectInputStream ois = new ObjectInputStream(fis);
			    playerGraph = (HashMap) ois.readObject();
			    ois.close();
			    fis.close();
				fis = new FileInputStream("playerIdMap.ser");
			    ois = new ObjectInputStream(fis);
			    playerIdMap = (HashMap) ois.readObject();
			    ois.close();
			    fis.close();
			 } catch(Exception e) {
				 buildGraph();
		     } 
		     preloadDone = true;
		     System.out.println("Preload Complete");
	    }
		// the start method that is needed to implemnet runnable 
		// this will just start the thread created in the class 
		public void start () {
			System.out.println("Starting " +  threadName );
		    if (t == null) {
		    	t = new Thread (this, threadName);
		        t.start ();
		    }
		}
	}
	
	// Initializing global variables
	String baseURL;
	HashMap<String, JSONObject> playerMap;
	HashMap<String, JSONObject> rookieMap;
	HashMap<String, JSONObject> defMap;
	
	ArrayList<String> headerNames;
	ArrayList<String> headerDef;
	LinkedList<String> path;

	HashMap<String, String> mvps = new HashMap<String, String>();
	HashMap<String, String> mvpLinks = new HashMap<String, String>();
	HashMap<String, String> link_mvp = new HashMap<String, String>();
	HashMap<String, HashSet<String>> player_teammates = new HashMap<String, HashSet<String>>();
	HashMap<String, String> parent_player = new HashMap<String, String>();
	HashMap<String, String> yrs_links = new HashMap<String, String>();
	ArrayList<String> years = new ArrayList<String>();
	HashMap<String, HashSet<String>> playerGraph = new HashMap<String, HashSet<String>>();
	HashMap<String, String> mvpIdMap =  new HashMap<String, String>();
	HashMap<String, String> mvpNameMap =  new HashMap<String, String>();
	HashMap<String, String> playerIdMap =  new HashMap<String, String>();
	HashMap<String, String> playerLink = new HashMap<String, String>();
	TableRowSorter<TableModel> mvpSorter;
	
	JFrame frame;
	JPanel panel;
	JScrollPane js;
	JTable table;
	JTable filteredTable;
	JLabel mvpFunction;
	Object[][] tableData;
	Object[] headers;
	Class[] types;
	Class[] displayTypes;
	
	Object[][] dpoyData;	
	JTable dpoyTable;
	Object[] headersDef;
	Class[] defTypes;
	JLabel dpoyFunction;
	
    double DefrtgCoE = 0.60;
    double DefrtgExp = 0.30;
    double defBlksCoE = 1.80;
    double defBlksExp = 0.30;
    double defStlCoE = 2.30;
    double defStlExp = 1.30;
    double dWSCoE = 2.90;
    double dWSExp = 2.90;
    double defWinCoE = 1.20;
    double defWinExp = 0.5;

	double pointsCoE = 1.40;
	double pointsExp = 1.00;
	double assistsCoE = 0.90;
	double assistsExp = 1.10;
	double rebsCoE = 0.90;
	double rebsExp = 0.70;
	double blocksCoE = 0.80;
	double blocksExp = 1.30;
	double stealsCoE = 1.00;
	double stealsExp = 1.20;
	double winsCoE = 0.90;
	double winsExp = 0.70;
	
	/*
	 * Constructor that initializes the base URL and loads the document produced
	 * from that URL
	 */

	public nbaStats() {
		this.baseURL = "http://stats.nba.com/stats/{endpoint}";
		HashMap<String, String> headers = new HashMap<String, String>();
		String refer = "https://www.nba.com/stats/players/traditional/";
		headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5)");
		headers.put("referer", refer);
		Map<String, Object> query = new TreeMap<String, Object>();
		
		query.put("MeasureType", "Base");
		query.put("PerMode", "PerGame");
		query.put("PlusMinus", "N");
		query.put("PaceAdjust", "N");
		query.put("Rank", "N");
		query.put("Season", "2020-21");
		query.put("PORound", "0");
		query.put("SeasonType", "Regular Season");
		query.put("Outcome","");
		query.put("Location", "");
		query.put("Month", "0");
		query.put("SeasonSegment", "");
		query.put("DateFrom", "");
		query.put("DateTo", "");
		query.put("OpponentTeamID", "0");
		query.put("VsConference", "");
		query.put("VsDivision", "");
		query.put("TeamID", "0");
		query.put("Conference", "");
		query.put("Division", "");
		query.put("GameSegment", "");
		query.put("Period", "0");
		query.put("ShotClockRange", "");
		query.put("GameScope", "");
		query.put("LastNGames", "0");
		query.put("PlayerPosition", "");
		query.put("PlayerExperience", "");
		query.put("StarterBench", "");
		query.put("DraftYear", "");
		query.put("DraftPick", "");
		query.put("College", "");
		query.put("Country", "");
		query.put("Height", "");
		query.put("Weight", "");
		query.put("LeagueID", "00");		
		query.put("TwoWay", "0");
		
		rookieMap = new HashMap<String, JSONObject>();
		playerMap = new HashMap<String, JSONObject>();
		defMap = new HashMap<String, JSONObject>();

		HttpRequest request = Unirest.get(baseURL).routeParam("endpoint", "leaguedashplayerstats").headers(headers).queryString(query);
		createMap(request, playerMap, "");
		query.put("PlayerExperience", "Rookie");
		Unirest.config().reset();
		HttpRequest requestRookies = Unirest.get(baseURL).routeParam("endpoint", "leaguedashplayerstats").headers(headers).queryString(query);
		createMap(requestRookies, rookieMap, "");
		Unirest.config().reset();
		Unirest.shutDown();
		
		headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_5)");		
		headers.put("referer", "https://www.nba.com/stats/players/defense/");
		query.put("PlayerExperience", "");
		query.put("MeasureType", "Defense");
		HttpRequest requestDef = Unirest.get(baseURL).routeParam("endpoint", "leaguedashplayerstats").headers(headers).queryString(query);
		createMap(requestDef, defMap, "def");
		//map made
		String mvp_link = "https://basketball.realgm.com/nba/awards/by_type";

		Document doc = null;
		Element table = null;
		try {
			URL url = new URL(mvp_link);
			doc = Jsoup.parse(url, 3000);
			table = doc.select("table.tablesaw").first();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
		Elements tr = table.select("tbody").select("tr");
    	String year = null;
    	String name = null;
    	String link = null;
		String id = null;
        for (Element el: tr) {
        		Element yearEl = el.child(0);
        		Element nameEl = el.child(1);
        		year = yearEl.attr("rel");
        		name = nameEl.text();
        		link = nameEl.child(0).attr("href");
				id = nameEl.child(0).attr("href").split("/")[4];
        		playerGraph.put(name, null);
    			mvps.put(name, year);
    			mvpLinks.put(name, link);
    			link_mvp.put(link, name);
    			yrs_links.put(year, link);
				mvpIdMap.put(id, name);
				mvpNameMap.put(name, id);
    			years.add(year);
        
        }
		BFSPreload bfsThread = new BFSPreload("Preload");
		bfsThread.start();
		// Initializing frame
	    frame = new JFrame();
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(1200, 800);
	    frame.setResizable(false);
	    frame.setVisible(true);
	}
	
	
	public void createMap(HttpRequest request, HashMap<String, JSONObject> map, String curMap) {
		try {
			HttpResponse<JsonNode> response = request.asJson();
			kong.unirest.json.JSONObject responsejson = response.getBody().getObject();
			// Accessing JSON Array containing desired data
			kong.unirest.json.JSONArray results = responsejson.getJSONArray("resultSets");
			// Accessing the JSON Object inside the array
			kong.unirest.json.JSONObject jsonObj = results.getJSONObject(0);
			// Acessing JSON Array that gives the headers
			kong.unirest.json.JSONArray jsonHeaders = jsonObj.getJSONArray("headers");
			// Accessing JSON Array that gives the players and their statistics
			kong.unirest.json.JSONArray jsonPlayers = jsonObj.getJSONArray("rowSet");
			
			// Initializing a HashMap of Hashmaps to map each player's name to their statistics
			// Initializing a list of headers
			ArrayList<String> curHeader = new ArrayList<String>();
			
			if (curMap.equals("def")) {
				headerDef = curHeader;
			} else {
				headerNames = curHeader;
			}
			// Iterating through all players
			for (int i = 0; i < jsonPlayers.length(); i++) {
				JSONObject player = new JSONObject();
				int nameIndex = -1;
				// Iterating through all headers/statistics
				for (int j = 0; j < jsonHeaders.length(); j++) {
					
					// If header is player name, save index and continue
					if (jsonHeaders.get(j).toString().equals("PLAYER_NAME")) {
						nameIndex = j;
						continue;
					}
					else {
						// Storing header names in list
						if (curHeader.size() < jsonHeaders.length() - 1) {
							curHeader.add(jsonHeaders.get(j).toString());
						}
						// Storing the player's statistics as an entry in an JSON object
						try {
							player.put(jsonHeaders.get(j).toString(), 
									jsonPlayers.getJSONArray(i).get(j));		
						} catch (org.json.JSONException e) {
							e.printStackTrace();
						}
					}
				}
				// Add player and their statistics to playerMap
				map.put(jsonPlayers.getJSONArray(i).get(nameIndex).toString(), player);
			}	
		} catch (UnirestException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
		
	// Building graph of all mvp players and their teammates starting at LeBron
	public void buildGraph() {
		HashSet<String> curr = new HashSet<String>();
		LinkedList<String> fifo = new LinkedList<String>();
		
		// Starting with LeBron, add begin building graph with players as nodes
		// And the players' teammates as neighbors
		String player1 = "LeBron James";
		String id = mvpNameMap.get(player1);
		String link = this.mvpLinks.get(player1);
		link = link.replaceFirst("Summary", "Teammates");
		String parentId = null;
		fifo.add(link);
		curr.add(id);
		String playerId = id;

		// Looping through all mvp candidates to add their teammates to the graph
		while(!curr.containsAll(mvpIdMap.keySet())) {
			
			link = fifo.pollFirst();
			parentId = playerId;
			// Creating new link to parse
			String[] split_string = link.split("/");
			String newLink = link.replaceFirst("Summary", "Teammates");
			playerId = split_string[4];

			link = newLink;
			
			String base = "https://basketball.realgm.com";
			Document doc = null;
			Element table = null;

			try {
				// Using JSoup to build a document from the new link
				String url = base + link;
				doc = Jsoup.connect(url).userAgent("Mozilla").data("name", "jsoup").get();
				table = doc.select("table.tablesaw").first();
			} catch (IOException e) {
				e.printStackTrace();
			}
			// Parsing through all teammates of that player through a table
			// And adding them to the graph by selecting specific elements
			Elements tr = table.select("tbody").select("tr");
			HashSet<String> team =  playerGraph.get(playerId)  == null ? new HashSet<String>() :  playerGraph.get(playerId);;
			for (Element el: tr) {
				Element nameEl = el.child(0);
				String childName = nameEl.text();
				String childLink = nameEl.child(0).attr("href");
				String childId = nameEl.child(0).attr("href").split("/")[4];
				team.add(childId);
				// Add player to teammate's neighbor list in graph
				playerIdMap.put(childId, childName);
				HashSet<String> playerCurTeam = playerGraph.get(childId)  == null ? new HashSet<String>() : playerGraph.get(childId);
				playerCurTeam.add(playerId);
				playerGraph.put(childId, playerCurTeam);
				
				// Add teammate to queue if teammate has not been added yet
				if (!curr.contains(childId) && childLink != null) {
					fifo.add(childLink);
					curr.add(childId);
				}
			}
			// Add player to graph
			playerGraph.put(playerId, team);
			if (!preloadDone) {
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}	

		}
		// Ensuring all teammates of each mvp is added to the graph
		for (String mvp : mvpIdMap.keySet()) {
			// Obtaining teammate link
			String curLink = mvpLinks.get(mvpIdMap.get(mvp));
			playerIdMap.put(mvp, mvpIdMap.get(mvp));
			curLink = curLink.replaceFirst("Summary", "Teammates");
			String[] split_string = curLink.split("/");
			playerId = split_string[4];
			
			String base = "https://basketball.realgm.com";
			Document doc = null;
			Element table = null;

			try {
				// Using JSoup to build a document from the new link
				String url = base + curLink;
				doc = Jsoup.connect(url).userAgent("Mozilla").data("name", "jsoup").get();
				table = doc.select("table.tablesaw").first();
			} catch (IOException e) {
				e.printStackTrace();
			}
			// Parsing through all teammates of that player through a table
			// And adding them to the graph by selecting specific elements
			Elements tr = table.select("tbody").select("tr");
			HashSet<String> teammates = new HashSet<String>();
			for (Element el: tr) {
				Element nameEl = el.child(0);
				String childName = nameEl.text();
				String childLink = nameEl.child(0).attr("href");
				String childId = nameEl.child(0).attr("href").split("/")[4];
				playerIdMap.put(childId, childName);
				teammates.add(childId);
			}
			// If player's teammates do not exist in the graph, add them
			HashSet<String> teammatesInMap = playerGraph.get(playerId);
			if (teammatesInMap == null) {
				teammatesInMap = new HashSet<String>();
			}
			// Add teammate to set, no duplicate check needed as Sets do not allow that
			for  (String teammate : teammates) {
				teammatesInMap.add(teammate);
			}
			// Add mvp node to the graph
			playerGraph.put(mvp, teammatesInMap);
		}
		
		// The try catch block that attempts to write the
		// graph created from traversing all MVP's teammates
		// to a file called playerGraph.ser
		// this is a way of decrease the total amount of time
		// needed to wait for the graph to build later on
		try {
                 FileOutputStream fos =
                    new FileOutputStream("playerGraph.ser");
                 ObjectOutputStream oos = new ObjectOutputStream(fos);
                 oos.writeObject(playerGraph);
                 oos.close();
                 fos.close();
                 fos =
                    new FileOutputStream("playerIdMap.ser");
                 oos = new ObjectOutputStream(fos);
                 oos.writeObject(playerIdMap);
                 oos.close();
                 fos.close();
                 System.out.printf("Serialized HashMap data is saved in playerGraph.ser");
          } catch (IOException ioe) {
                 ioe.printStackTrace();
          }
		System.out.println("Graph Building Complete");
	}
	
	// Method to build path from one target mvp to another through teammates
	public LinkedList<String> BFS (String player1, String player2) {
		HashSet<String> curr = new HashSet<String>();
		LinkedList<String> ret = new LinkedList<String>();
		HashMap<String, String> playerParent = new HashMap<String, String>();
		LinkedList<String> fifo = new LinkedList<String>();
		
		if (player1 == null || player2 == null) {
			return ret;
		}
	
		String parentId = null;
		String currentPlayer = player1;
		String currentPlayerId = mvpNameMap.get(player1);
		String targetPlayerId = mvpNameMap.get(player2);
		fifo.add(currentPlayerId);
		curr.add(currentPlayerId);
		// While visited players does not include target player and queue is not empty,
		// iterate through the graph
		while(!curr.contains(targetPlayerId) && !fifo.isEmpty()) {
			parentId = currentPlayerId;
			currentPlayerId = fifo.pollFirst();
			
			HashSet<String> teammates = playerGraph.get(currentPlayerId);
			if (teammates == null) {
				continue;
			}
			// For each teammate add them and current player to parent array list
			// and add them to the queue as well as the visited
			for (String teammate : teammates) {
				if (!curr.contains(teammate)) {
					playerParent.put(teammate, currentPlayerId);
					fifo.add(teammate);
					curr.add(teammate);
				}
			}
		}
		// If target player is not found after iterating the graph, no path is found
		if (!curr.contains(targetPlayerId)) {
			ret.addFirst("NO PATH FOUND");
			return ret;
		}
		// Otherwise build path from target player to starting player 
		// by backtracking through parent array list
		String parentTrack = targetPlayerId;
		while(!parentTrack.equals(mvpNameMap.get(player1))) {
			// Add each parent to the beginning of path list
			ret.addFirst(playerIdMap.get(parentTrack));
			parentTrack = playerParent.get(parentTrack);
		}
		// Add starting player as the first player in the path
		ret.addFirst(player1);
		return ret;
	}
	
	// Creating panel for MVP-BFS button
	public void makeBFSFrame() {
		path = new LinkedList<String>();
		if (js != null) {
			panel.remove(js);
		}
		// Adding panel to select starting player
		JPanel player1Panel = new JPanel();
		JLabel player1 = new JLabel("SELECT PLAYER 1");
		// Adding mvps
		JList<String> player1Selector = new JList<String>(mvps.keySet().toArray(new String[mvps.size()]));
		player1.setSize(10, 10);
		player1Panel.add(player1);
		player1Panel.add(player1Selector);
		
		// Adding panel to select target player
		JPanel player2Panel = new JPanel();
		JLabel player2 = new JLabel("SELECT PLAYER 2");
		player2.setSize(10, 10);
		// Adding mvps
		JList<String> player2Selector = new JList<String>(mvps.keySet().toArray(new String[mvps.size()]));
		player2Panel.add(player2);
		player2Panel.add(player2Selector);
		
		// Adding additional button and label
		JButton startBFS = new JButton("Find Path");
		startBFS.setSize(10,10);
		JLabel pathLabel = new JLabel("MAKE A PATH");
		startBFS.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// If players have been loaded, find path, otherwise print error message
				if (preloadDone) {
					pathLabel.setText("Searching");
					panel.repaint();
					path = BFS(player1Selector.getSelectedValue(), player2Selector.getSelectedValue());
					// Print path where the path label is
					pathLabel.setText(path.toString());
				} else {
					pathLabel.setText("Preload not complete");
				}
				
			}
			
		});
		
		// Creating panel and formatting it with constraints
		// as well as adding this panel to the big panel
		JPanel selectorPanel = new JPanel();
		selectorPanel.add(player1Panel);
		selectorPanel.add(player2Panel);
		selectorPanel.add(startBFS);
		
	    GridBagConstraints constraint = new GridBagConstraints();
	    constraint.weightx = 2.0;
	    constraint.weighty = 1.0;
	    constraint.gridx = 10;
	    constraint.gridy = 40;
	    constraint.gridwidth = 20;
	    constraint.gridheight= 40;
	    panel.add(pathLabel, constraint);
		
	    constraint.weightx = 2.0;
	    constraint.weighty = 1.0;
	    constraint.gridx = 0;
	    constraint.gridy = 0;
	    constraint.gridwidth = 20;
	    constraint.gridheight= 20;
	    panel.add(selectorPanel, constraint);
		panel.repaint();
	}
	
	// Prints out each player's name and their statistic
	public void accessingAnyStatOfAllPlayers(String statistic) {
		for (String playerName : this.playerMap.keySet()) {
			try {
				System.out.println(playerName + " " + this.playerMap.get(playerName).get(statistic));
			} catch (org.json.JSONException e) {
				e.printStackTrace();
				System.out.println("Please enter a valid statistic name!");
				break;
			}
		}	    
	}
	
	// Method to remove a column from JTable
	private void removeColumn(int index, JTable table, boolean filter, Class[] types) {
	    int rows = table.getRowCount();
	    int cols = table.getColumnCount() - 1;
	    
	    Object[][] oldData = new Object[rows][cols];
	    String[] newCol = new String[cols];
	
	    for (int j = 0; j < cols; j++) {
	    	// If index is not target index, maintain data
	        if (j < index) {
	            newCol[j] = table.getColumnName(j);
	            for (int i = 0; i < rows; i++) {
	                oldData[i][j] = table.getValueAt(i, j);
	            }
	        } else {
	        	// Otherwise, shift data to cover the target column
	            newCol[j] = table.getColumnName(j + 1);
	            for (int i = 0; i < rows; i++) {
	                oldData[i][j] = table.getValueAt(i, j + 1);
	            }
	        }
	    }
	    
	    // Building a new table model from the shifted data using new class
	    DefaultTableModel newModel = new DefaultTableModel(oldData, newCol);
		if (filter) {
		    newModel = (new DefaultTableModel(oldData, newCol) {
		    	@Override
		    	public boolean isCellEditable(int row, int column) {
		    		return false;
		    	}
		    	@Override
		    	public Class getColumnClass(int columnIndex) {
		    		return types[columnIndex];
		    	}
		    });
		}
	    table.setModel(newModel);       
	}
		
    // Creating DPOY table
	public void displayDpoy() {
		dpoyData = new Object[this.playerMap.keySet().size()]
	    		[headerNames.size() + 1];
	    // Creating table headers
		headersDef = new Object[headerDef.size() + 1];
		
	    int index = 0;
	    // Filling in table
	    for (String player : defMap.keySet()) {
	    	JSONObject playerStats = defMap.get(player);
	    	dpoyData[index][0] = player;
	    	for (int i = 0; i < headerDef.size(); i++) {
	    		// Adding headers
	    		headersDef[i + 1] = headerDef.get(i);
		    	try {
		    		// Adding data
		    		dpoyData[index][i + 1] = playerStats.get(headerDef.get(i));
				} catch (org.json.JSONException e) {
					e.printStackTrace();
				}
		    }
	    
	        index++;
	    }
	    headersDef[0] = "PLAYER";
	    headersDef[3] = "TEAM";

		// Creating a duplicate table of the first table in order to 
		// filter out non-MVP caliber players
		dpoyTable = new JTable(dpoyData, headersDef);
		
		defTypes = new Class[headersDef.length + 1];
    	for (int i = 0; i < headersDef.length + 1; i++) {
    		if (i == 2 || i == 1|| i == 3 || i == 5 || i == 6 || i == 7) {
    			defTypes[i] = Integer.class;
    		} else if (i == 0) {
    			defTypes[i] = String.class;
			} else {
				defTypes[i] = Double.class;	
    		}
    	}
    	
    	// Setting table model for the new table
    	dpoyTable.setModel(new DefaultTableModel(dpoyData, headersDef) {
    		@Override
	    	public boolean isCellEditable(int row, int column) {
	    	    return false;
	    	}
		    @Override
		    public Class getColumnClass(int columnIndex) {
		        return defTypes[columnIndex];
		    }
		});
    	
    	dpoyTable.setAutoCreateRowSorter(true);

	    // Formatting filtered table by removing extra stats columns
		while(dpoyTable.getColumnCount() > 23) {
	    	removeColumn(23, dpoyTable, true, defTypes);
	    }
		
		// Removing player and team ID columns
		removeColumn(1, dpoyTable, true, defTypes);
		removeColumn(1, dpoyTable, true, defTypes);

	    dpoyTable.setRowHeight(50);

	    // Removing players with less than 40 games or 28 minutes a game
		for (int i = 0; i < dpoyTable.getRowCount(); i++) {
			if (Integer.parseInt(dpoyTable.getValueAt(i, 3).toString()) < 40 
					|| Double.parseDouble(dpoyTable.getValueAt(i, 7).toString()) < 28) {
				((DefaultTableModel)dpoyTable.getModel()).removeRow(i);
				i--;
			}
		}    
	    
		// Removing main table
		panel.remove(js);
		
		// Calculate dpoy candidates
        statsPickerDpoy();
        
        // Formatting table as well as setting constraints to help with formatting
		js = new JScrollPane(dpoyTable);
		dpoyTable.getColumnModel().getColumn(0).setPreferredWidth(100);
		dpoyTable.getTableHeader().setFont(new Font("Arial", Font.PLAIN, 12));
	    dpoyTable.setFont(new Font("Arial", Font.PLAIN, 12));
	    for (int i = 1; i < dpoyTable.getColumnCount(); i++) {
	    	dpoyTable.getColumnModel().getColumn(i).setPreferredWidth(60);
	    }

		GridBagConstraints constraint = new GridBagConstraints();
		constraint.weightx = 2.0;
	    constraint.weighty = 1.0;
	    constraint.fill = GridBagConstraints.BOTH;
	    constraint.gridx = 0;
	    constraint.gridy = 0;
	    constraint.gridwidth = 20;
	    constraint.gridheight= 3;
	    dpoyTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	    panel.add(js, constraint);

		constraint.weightx = 2.0;
	    constraint.weighty = 1.0;
	    constraint.fill = GridBagConstraints.BOTH;
	    constraint.gridx = 2;
	    constraint.gridy = 20;
	    constraint.gridwidth = 20;
	    constraint.gridheight= 3;
	    
	    // Initializing coefficients and exponents to numbers that we see fit
	 	// These are modifiable using sliders
	    DefrtgCoE = 0.60;
	    DefrtgExp = 0.30;
	    defBlksCoE = 1.80;
	    defBlksExp = 0.30;
	    defStlCoE = 2.30;
	    defStlExp = 1.30;
	    dWSCoE = 2.90;
	    dWSExp = 2.90;
	    defWinCoE = 1.20;
	    defWinExp = 0.5;
	    
	    // Initializing the string function to print the function and weights
		String function = String.format("    DPOY SCORE = %.2f*DEF_RTG^%.2f "
				+ "+ %.2f*STEALS^%.2f + %.2f*BLOCKS^%.2f + %.2f*DEF_WS^%.2f\" + %.2f*WINS^%.2f", 
				DefrtgCoE, DefrtgExp, defStlCoE, defStlExp, defBlksCoE, 
				defBlksExp, dWSCoE, dWSExp, defWinCoE, defWinExp);
		dpoyFunction = new JLabel(function);
		
		panel.add(dpoyFunction, constraint);
		panel.revalidate();
		panel.repaint();
		dpoyCalculate(); 

	}
	
	public void statsPickerDpoy() {
		
		// Initializing sliders panel
		JPanel sliders = new JPanel(new GridLayout(5, 1));

		// Creating a new panel for this statistic
		JPanel dfrtgPanel = new JPanel(new GridLayout(4, 1));
		JSlider dfrtgS = new JSlider(0, 40);
		JSlider dfrtgP = new JSlider(0, 40);
		dfrtgS.setValue((int) (DefrtgCoE * 10));
		dfrtgP.setValue((int) (DefrtgExp * 10));
		JLabel  dfrtgSLabel = new JLabel(String.format("Defensive Rating Coefficent: %.2f", DefrtgCoE));
		JLabel  dfrtgExpo = new JLabel(String.format("Defensive Rating Exponent %.2f", DefrtgExp));
		
		// Linking slider to actions
		dfrtgS.addChangeListener(new ChangeListener() {
			 public void stateChanged(ChangeEvent e) {
				DefrtgCoE = (double) dfrtgS.getValue() / 10.0;
				dfrtgSLabel.setText(String.format("Defensive Rating Coefficent %.2f", DefrtgCoE));
				dpoyCalculate(); 
				
			}
		});
		
		// Linking slider to actions
		dfrtgP.addChangeListener(new ChangeListener() {
			 public void stateChanged(ChangeEvent e) {
				DefrtgExp = (double) dfrtgP.getValue() / 10.0;
				dfrtgExpo.setText(String.format("Defensive Rating Exponent %.2f", DefrtgExp));
				dpoyCalculate(); 
			}
		});
		
		// Setting slider bounds
		dfrtgS.setBounds(0, 0, 100, 10);
		dfrtgP.setBounds(0, 0, 100, 10);
		
		// Formatting slider panel
		dfrtgPanel.add(dfrtgSLabel);
		dfrtgPanel.add(dfrtgS);
		dfrtgPanel.add(dfrtgExpo);
		dfrtgPanel.add(dfrtgP);
				
		// Creating a new panel for this statistic
		JPanel  blkPanel = new JPanel(new GridLayout(4, 1));		
		JSlider blkS = new JSlider(0, 40);
		JSlider blkP = new JSlider(0, 40);
		blkS.setValue((int) (defBlksCoE * 10));
		blkP.setValue((int) (defBlksExp * 10));
		JLabel  blkSLabel = new JLabel(String.format("Blocks Coefficent: %.2f", defBlksCoE));
		JLabel  blkPLabel = new JLabel(String.format("Blocks Coefficent: %.2f", defBlksExp));
		
		// Linking slider to actions
		blkS.addChangeListener(new ChangeListener() {
			 public void stateChanged(ChangeEvent e) {
				defBlksCoE = (double) blkS.getValue() / 10.0;
				blkSLabel.setText(String.format("Blocks Coefficent: %.2f", defBlksCoE));
				dpoyCalculate(); 
			}
		});
		
		// Linking slider to actions
		blkP.addChangeListener(new ChangeListener() {
			 public void stateChanged(ChangeEvent e) {
				defBlksExp = (double) blkP.getValue() / 10.0;
				blkPLabel.setText(String.format("Blocks Coefficent: %.2f", defBlksExp));
				dpoyCalculate(); 
			}
		});
		
		// Setting slider bounds
		blkS.setBounds(0, 0, 100, 10);
		blkP.setBounds(0, 0, 100, 10);
		
		// Formatting slider panel
		blkPanel.add(blkSLabel);
		blkPanel.add(blkS);
		blkPanel.add(blkPLabel);
		blkPanel.add(blkP);
				
		// Creating a new panel for this statistic
		JPanel  stlPanel = new JPanel(new GridLayout(4, 1));		
		JSlider stlS = new JSlider(0, 40);
		JSlider stlP = new JSlider(0, 40);
		stlS.setValue((int) (defStlCoE * 10));
		stlP.setValue((int) (defStlExp * 10));
		JLabel  stlSLabel = new JLabel(String.format("Steals Coefficent: %.2f", defStlCoE));
		JLabel  stlPLabel = new JLabel(String.format("Steals Exponent: %.2f", defStlExp));
		
		// Linking slider to actions
		stlS.addChangeListener(new ChangeListener() {
			 public void stateChanged(ChangeEvent e) {
				 defStlCoE = (double) stlS.getValue() / 10.0;
				stlSLabel.setText(String.format("Steals Coefficent: %.2f", defStlCoE));
				dpoyCalculate(); 
			}
		});
		
		// Linking slider to actions
		stlP.addChangeListener(new ChangeListener() {
			 public void stateChanged(ChangeEvent e) {
				defStlExp = (double) stlP.getValue() / 10.0;
				stlPLabel.setText(String.format("Steals Exponent: %.2f", defStlExp));
				dpoyCalculate(); 
			}
		});
		
		// Setting slider bounds
		stlS.setBounds(0, 0, 100, 10);
		stlP.setBounds(0, 0, 100, 10);
		
		// Formatting slider panel
		stlPanel.add(stlSLabel);
		stlPanel.add(stlS);
		stlPanel.add(stlPLabel);
		stlPanel.add(stlP);
		
		// Creating a new panel for this statistic
		JPanel  dWSPanel = new JPanel(new GridLayout(4, 1));
		JSlider dWSS = new JSlider(0, 40);
		JSlider dWSP = new JSlider(0, 40);
		dWSS.setValue((int) (dWSCoE * 10));
		dWSP.setValue((int)(dWSExp * 10));
		JLabel  dWSSLabel = new JLabel(String.format("Defensive Win Shares Coefficent: %.2f", dWSCoE));
		JLabel  dWSPExpo = new JLabel(String.format("Defensive Win Shares Exponent: %.2f", dWSExp));
		
		// Linking slider to actions
		dWSS.addChangeListener(new ChangeListener() {
			 public void stateChanged(ChangeEvent e) {
				 dWSCoE = (double) dWSS.getValue() / 10.0;
				dWSSLabel.setText(String.format("Defensive Win Shares Coefficent: %.2f", dWSCoE));
				dpoyCalculate(); 

			}
		});
		
		// Linking slider to actions
		dWSP.addChangeListener(new ChangeListener() {
			 public void stateChanged(ChangeEvent e) {
				 dWSExp = (double) dWSP.getValue() / 10.0;
				dWSPExpo.setText(String.format("Defensive Win Shares Exponent: %.2f", dWSExp));
				dpoyCalculate(); 
			}
		});
		
		// Setting slider bounds
		dWSP.setBounds(0, 0, 100, 10);
		dWSP.setBounds(0, 0, 100, 10);
		
		// Formatting slider panel
		dWSPanel.add(dWSSLabel);
		dWSPanel.add(dWSS);
		dWSPanel.add(dWSPExpo);
		dWSPanel.add(dWSP);
	
		// Creating a new panel for this statistic
		JPanel  winPanel = new JPanel(new GridLayout(4, 1));		
		JSlider winS = new JSlider(0, 40);
		JSlider winP = new JSlider(0, 40);
		winS.setValue((int) (defWinCoE * 10));
		winP.setValue((int) (defWinExp * 10));
		JLabel  winLabel = new JLabel(String.format("Wins Coefficent: %.2f", defWinCoE));
		JLabel 	winPLabel = new JLabel(String.format("Wins Exponent: %.2f", defWinExp));
		
		// Linking slider to actions
		winS.addChangeListener(new ChangeListener() {
			 public void stateChanged(ChangeEvent e) {
				defWinCoE = (double) winS.getValue() / 10.0;
				winLabel.setText(String.format("Wins Coefficent: %.2f", defWinCoE));
				dpoyCalculate(); 
			}
		});
		
		// Linking slider to actions
		winP.addChangeListener(new ChangeListener() {
			 public void stateChanged(ChangeEvent e) {
				defWinExp = (double) winP.getValue() / 10.0;
				winPLabel.setText(String.format("Wins Exponent: %.2f", defWinExp));
				dpoyCalculate(); 
			}
		});
		// Setting slider bounds
		winS.setBounds(0, 0, 100, 10);
		winP.setBounds(0, 0, 100, 10);
		
		// Formatting slider panel
		winPanel.add(winLabel);
		winPanel.add(winS);
		winPanel.add(winPLabel);
		winPanel.add(winP);
		
		// Adding each stat panel to the sliders panel		
		sliders.add(dfrtgPanel);
		sliders.add(blkPanel);
		sliders.add(stlPanel);
		sliders.add(dWSPanel);
		sliders.add(winPanel);
		
		// Formatting sliders panel through constraints
		GridBagConstraints constraint = new GridBagConstraints();
	    constraint.weightx = .2;
	    constraint.weighty = .1;
	    constraint.gridx = 20;
	    constraint.gridy = 0;
	    constraint.gridwidth = 20;
	    constraint.gridheight= 3;
		constraint.fill = GridBagConstraints.NONE;
		
		// Adding sliders panel and constraints to full panel
	    panel.add(sliders, constraint);
		panel.repaint();
		panel.revalidate();
	}

	// Setting dpoy column has been added to true
	boolean dpoyColSet = true;
	
	// Calculate dpoy scores
	public void dpoyCalculate() {
		
		// Initializing the string function to print the function and weights
		String function = String.format("    DPOY SCORE = %.2f*DEF_RTG^%.2f "
				+ "+ %.2f*STEALS^%.2f + %.2f*BLOCKS^%.2f + %.2f*DEF_WS^%.2f\" + %.2f*WINS^%.2f", 
				DefrtgCoE, DefrtgExp, defStlCoE, defStlExp, defBlksCoE, defBlksExp, 
				dWSCoE, dWSExp, defWinCoE, defWinExp);
			
		
		DefaultTableModel model = (DefaultTableModel) dpoyTable.getModel();
		
		// Adding DPOY Score column to table
		if (dpoyColSet) {
			dpoyColSet = false;
			model.addColumn(" DPOY Score");
			dpoyTable.moveColumn(model.getColumnCount() - 1, 1);
			TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) dpoyTable.getRowSorter();
			sorter.toggleSortOrder(model.getColumnCount() -1);
			sorter.toggleSortOrder(model.getColumnCount() -1);
			dpoyTable.setRowSorter(sorter);
		}
		dpoyFunction.setText(function);
		
		// Iterating through each rookie and obtaining desired stats
		for (int i = 0; i < model.getRowCount(); i++) {
			String playerName = model.getValueAt(i, 0).toString();
			JSONObject player = defMap.get(playerName);

			double playerDfrtg = 0;
			double playerBlocks  = 0;
			double playerSteals  = 0;
			double playerDWS  = 0;
			double playerWins  = 0;
			int playerGP = 0;
			
			try {
				playerDfrtg = (double) player.getDouble("DEF_RATING");
				playerBlocks = (double) player.getDouble("BLK");
				playerSteals = (double) player.getDouble("STL");
				playerWins = (double) player.getInt("W");
				playerDWS  = (double) player.getDouble("DEF_WS");;
				playerGP = (int) player.getInt("GP");
			} catch (JSONException e) {
				
				System.out.println(e);
			}
			
			// Calculating dpoy score based on obtained stats
			playerDfrtg = 120 - playerDfrtg;
			playerDWS++;
			double dfrtgVal = DefrtgCoE * Math.pow(playerDfrtg, DefrtgExp);
			double dwsVal = dWSCoE * Math.pow(playerDWS, dWSExp);
			double blocksVal = defBlksCoE * Math.pow(playerBlocks, defBlksExp);
			double stealsVal = defStlCoE * Math.pow(playerSteals, defStlExp);
			double winsVal = playerGP/72.0 * defWinCoE * Math.pow(playerWins, defWinExp);
			double dpoyScore = dfrtgVal + dwsVal + blocksVal + stealsVal + winsVal;
			model.setValueAt(dpoyScore, i, model.getColumnCount() - 1);
		}
		
		// Reformatting table and panel
		model.fireTableDataChanged();
		panel.repaint();
		panel.revalidate();
	}	

	// Creating starting table of all current nba players
	public void displayTable() {
		mvpColSet = true;
		dpoyColSet = true;
		if (js != null) {
			if (mvpFunction != null) {
				panel.remove(mvpFunction);
			}
			if (dpoyFunction != null) {
				panel.remove(dpoyFunction);
			}
			frame.remove(panel);
		}
		// Creating table
	    tableData = new Object[this.playerMap.keySet().size()]
	    		[headerNames.size() + 1];
	    // Creating table headers
	    headers = new Object[headerNames.size() + 1];
		
	    int index = 0;
	    // Filling in table
	    for (String player : this.playerMap.keySet()) {
	    	JSONObject playerStats = this.playerMap.get(player);
	    	tableData[index][0] = player;
	    	for (int i = 0; i < this.headerNames.size(); i++) {
	    		// Adding headers
	    		headers[i + 1] = this.headerNames.get(i);
		    	try {
		    		// Adding data
					tableData[index][i + 1] = playerStats.get(this.headerNames.get(i));
				} catch (org.json.JSONException e) {
					e.printStackTrace();
				}
		    }
	    
	        index++;
	    }
	    
	    // Renaming player name and team abbreviation columns
		headers[0] = "PLAYER";
	    headers[3] = "TEAM";
	    
	    // Initializing table
	    this.table = new JTable(tableData, headers);
	    displayTypes = new Class[headers.length + 1];
    	for (int i = 0; i < headers.length + 1; i++) {
    		if (i == 3 || i == 4 || i == 5) {
    			displayTypes[i] = Integer.class;
    		} else if (i == 0 | i == 1 ) {
    			displayTypes[i] = String.class;
			} else {
				displayTypes[i] = Double.class;	
    		}
    	}

    	// Setting table model for the new table
        table.setModel(new DefaultTableModel(tableData, headers) {
	    	   @Override
	    	   public boolean isCellEditable(int row, int column) {
	    	       return false;
	    	   }
		    @Override
		    public Class getColumnClass(int columnIndex) {
		        return displayTypes[columnIndex];
		    }
		});

        // Deleting columns with extra statistics 
        table.setAutoCreateRowSorter(true);
	    while(table.getColumnCount() > 30) {
	    	removeColumn(30, table, false, displayTypes);
	    }
	    // Renaming player name and team abbreviation columns
		removeColumn(1, table, false, displayTypes);
		removeColumn(1, table, false, displayTypes);
		
		// Formatting table rows
	    this.table.setRowHeight(50);
	    
	    // Initializing horizontal scroll bar
	    this.js = new JScrollPane(this.table);

	    
	    // Initializing button panel and buttons
	    JPanel buttonPanel = new JPanel(new GridLayout(4, 1));
	    buttonPanel.setPreferredSize(new Dimension(200, 200));
	    buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());  
	    panel = new JPanel();
	    panel.setLayout(new GridBagLayout());
		panel.setSize(1200, 800);

	    JButton mvp = new JButton("MVP");
	    JButton dpoy = new JButton("DPOY");
	    JButton roty = new JButton("ROTY");
	    JButton mvpBfs = new JButton("MVP-BFS");
		JButton reset = new JButton("RESET");
		
		// Linking each button to its respective method
		mvp.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
			displayTable();
			tablePrune(playerMap.keySet(), "MVP");
         }
      	});
		
		dpoy.addActionListener(new ActionListener() {
	         public void actionPerformed(ActionEvent e) {
				displayTable();
				displayDpoy();
	         }
	    });
		
		roty.addActionListener(new ActionListener() {
	         public void actionPerformed(ActionEvent e) {
				displayTable();
				tablePrune(rookieMap.keySet(), "ROTY");
	         }
	    });
		
		mvpBfs.addActionListener(new ActionListener() {
	         public void actionPerformed(ActionEvent e) {
	        	displayTable();
				makeBFSFrame();

	         }
	      	});

		reset.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            displayTable();
         }
      	});

		// Adding buttons to button panel
	    buttonPanel.add(mvp);
	    buttonPanel.add(dpoy);
	    buttonPanel.add(roty);
	    buttonPanel.add(mvpBfs);
		buttonPanel.add(reset);
		
		// Formatting panel by setting and using constraints
	    GridBagConstraints constraint = new GridBagConstraints();
	    constraint.weightx = 2.0;
	    constraint.weighty = 1.0;
	    constraint.fill = GridBagConstraints.BOTH;
	    constraint.gridx = 0;
	    constraint.gridy = 0;
	    constraint.gridwidth = 20;
	    constraint.gridheight= 0;
	    panel.add(js, constraint);
	    
	    constraint.weightx = .2;
	    constraint.weighty = 1.0;
	    constraint.gridx = 20;
	    constraint.gridy = 10;
	    constraint.gridwidth = 1;
	    constraint.gridheight= 0;
		constraint.fill = GridBagConstraints.NONE;
	    panel.add(buttonPanel, constraint);
	    
	    // Formatting panel and frame
	    this.table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	    frame.add(panel);
		panel.revalidate();
		panel.repaint();
		frame.revalidate();
		frame.repaint();
		
	    //Setting column width to see data better
		int colCount = this.table.getColumnCount();
	    this.table.getColumnModel().getColumn(0).setPreferredWidth(100);
	    this.table.getTableHeader().setFont(new Font("Arial", Font.PLAIN, 12));
	    this.table.setFont(new Font("Arial", Font.PLAIN, 12));
	    for (int i = 1; i < colCount; i++) {
	    	this.table.getColumnModel().getColumn(i).setPreferredWidth(60);
	    }
	}
	
	// Pruning table to narrow down mvp/roty candidates
	public void tablePrune(Set<String> set, String pruneType) {
		
		// Initializing coefficients and exponents to numbers that we see fit
		// These are modifiable using sliders
		pointsCoE = 1.40;
		pointsExp = 1.00;
		assistsCoE = 0.90;
		assistsExp = 1.10;
		rebsCoE = 0.90;
		rebsExp = 0.70;
		blocksCoE = 0.80;
		blocksExp = 1.30;
		stealsCoE = 1.00;
		stealsExp = 1.20;
		winsCoE = 0.90;
		winsExp = 0.70;

		// Initializing means and deviations
		double pointMean = 0;
		double assistMean = 0;
		double blockMean = 0;
		double rebMean = 0;
		double stealMean = 0;
		
		double pointDev = 0;
		double assistDev = 0;
		double blockDev = 0;
		double rebDev = 0;
		double stealDev = 0;
		
		// Initializing lists to store stats for calculations
		ArrayList<Double> ptsList = new ArrayList<Double>();
		ArrayList<Double> astList = new ArrayList<Double>();
		ArrayList<Double> rebList = new ArrayList<Double>();
		ArrayList<Double> blkList = new ArrayList<Double>();
		ArrayList<Double> stlList = new ArrayList<Double>();
		
		// Creating a duplicate table of the first table in order to 
		// filter out non-MVP caliber players
		filteredTable = new JTable(tableData, headers);
		
		// Assigning each column its correct data class 
		types = new Class[headers.length + 1];
    	for (int i = 0; i < headers.length + 1; i++) {
    		if (i == 1 || i == 2 || i == 3 || i == 5 || i == 6 || i == 7) {
    			types[i] = Integer.class;
    		} else if (i == 0) {
				types[i] = String.class;
			} else {
				types[i] = Double.class;	
    		}
    	}

    	// Setting model for duplicate table
        filteredTable.setModel(new DefaultTableModel(tableData, headers) {
        	// Makes table uneditable
	    	@Override
	    	public boolean isCellEditable(int row, int column) {
	    	    return false;
	    	}
	    	// Returns column class to properly set a corresponding comparator
		    @Override
		    public Class getColumnClass(int columnIndex) {
		        return types[columnIndex];
		    }
		});
        
        // Removing all non-rookies if we are pruning for rotys
		for (int i = 0; i < filteredTable.getRowCount(); i++) {
		
			if (!set.contains(filteredTable.getValueAt(i, 0))) {
				((DefaultTableModel)filteredTable.getModel()).removeRow(i);
				i--;
			}
		}
		filteredTable.setAutoCreateRowSorter(true);
		
		// Iterating through the table and storing each traditional stat
		// to its corresponding list
		for (int i = 0; i < this.table.getColumnCount(); i++) {
			if (this.table.getColumnName(i).equals("PTS")) {
				for (int j = 0; j < this.table.getModel().getRowCount(); j++)
				{
					// Get all row values at column index 0
					double pts = Double.parseDouble(table.getValueAt(j, i).toString());
					ptsList.add(pts); 
				}
			}
			if (this.table.getColumnName(i).equals("AST")) {
				for (int j = 0; j < this.table.getModel().getRowCount(); j++) {
					// Get all row values at column index 0
					astList.add(Double.parseDouble(table.getValueAt(j, i).toString())); 
				}
			}
			
			if (this.table.getColumnName(i).equals("REB")) {
				for (int j = 0; j < this.table.getModel().getRowCount(); j++) {
					// Get all row values at column index 0
					rebList.add(Double.parseDouble(table.getValueAt(j, i).toString())); 
				}
			}
			
			if (this.table.getColumnName(i).equals("BLK")) {
				for (int j = 0; j < this.table.getModel().getRowCount(); j++) {
					// Get all row values at column index 0
					blkList.add(Double.parseDouble(table.getValueAt(j, i).toString())); 
				}
			}
			
			if (this.table.getColumnName(i).equals("STL")) {
				for (int j = 0; j < this.table.getModel().getRowCount(); j++) {
					// Get all row values at column index 0
					stlList.add(Double.parseDouble(table.getValueAt(j, i).toString())); 
				}
			}
		}
		
		// For each traditional stat, calculate the mean
		for (double pts : ptsList) {
			pointMean += pts;
	    }
		pointMean = pointMean / ptsList.size();
		
		for (double ast : astList) {
			assistMean += ast;
	    }
		assistMean = assistMean / astList.size();
		
		for (double blk : blkList) {
			blockMean += blk;
	    }
		blockMean = blockMean / blkList.size();
		
		for (double reb : rebList) {
			rebMean += reb;
	    }
		rebMean = rebMean / rebList.size();
		
		for (double stl : stlList) {
			stealMean += stl;
	    }
		stealMean = stealMean / stlList.size();	
		
		// Iterating through the table to find the (value - mean) portion of
		// the standard deviation for each traditional stat
		for (int i = 0; i < this.table.getColumnCount(); i++) {
			if (this.table.getColumnName(i).equals("PTS")) {
				for (int j = 0; j < this.table.getModel().getRowCount(); j++)
				{
					pointDev += Math.pow((Double.parseDouble
							(this.table.getValueAt(j, i).toString()) - pointMean), 2); 
				}
			}
			if (this.table.getColumnName(i).equals("AST")) {
				for (int j = 0; j < this.table.getModel().getRowCount(); j++)
				{
					assistDev += Math.pow((Double.parseDouble
							(this.table.getValueAt(j, i).toString()) - assistMean), 2); 
				}
			}
			
			if (this.table.getColumnName(i).equals("REB")) {
				for (int j = 0; j < this.table.getModel().getRowCount(); j++)
				{
					rebDev += Math.pow((Double.parseDouble
							(this.table.getValueAt(j, i).toString()) - rebMean), 2); 
				}
			}
			
			if (this.table.getColumnName(i).equals("BLK")) {
				for (int j = 0; j < this.table.getModel().getRowCount(); j++)
				{
					// Get all row values at column index 0
					blockDev += Math.pow((Double.parseDouble
							(this.table.getValueAt(j, i).toString()) - blockMean), 2); 
				}
			}
			
			if (this.table.getColumnName(i).equals("STL")) {
				for (int j = 0; j < this.table.getModel().getRowCount(); j++)
				{
					// Get all row values at column index 0
					stealDev += Math.pow((Double.parseDouble
							(this.table.getValueAt(j, i).toString()) - stealMean), 2); 
				}
			}	
		}
		
		// Dividing the (value - mean) total for each traditional stat size by the number of 
		// players in order to calculate standard deviation for each stat
		pointDev = Math.sqrt((pointDev / ptsList.size()));
		assistDev = Math.sqrt((assistDev / astList.size()));
		rebDev = Math.sqrt((rebDev / rebList.size()));
		blockDev = Math.sqrt((blockDev / blkList.size()));
		stealDev = Math.sqrt((pointDev / stlList.size()));
		
		// Initializing z-score is less than threshold booleans
		boolean pointZ = false;
		boolean assistZ = false;
		boolean rebZ = false;
		boolean blkZ = false;
		boolean stealZ = false;
		
		// Initializing thresholds for each statistic
		double pointsThresh = 0;
		double assistThresh = 0;
		double rebsThresh = 0;
		double blksThresh = 0;
		double stealsThresh = 0;
		
		// Setting threshold values based on whether or not we are selecting "MVPs" or not
		if (pruneType.equals("MVP")) {
			pointsThresh = 2.3;
			assistThresh = 3.5;
			blksThresh = 10;
			stealsThresh = 10;
		} else {
			pointsThresh = 1;
			assistThresh = 1;
			blksThresh = 6;
			stealsThresh = 6;
		}
		
		// Iterating through the players and checking if they are in the top echelon
		// of any traditional stat. If not, filter the player out of the table
		for (int i = 0; i < filteredTable.getModel().getRowCount(); i++) {

			// Boolean variables to keep track of whether the player is not in the 
			// top echelon of players for each traditional stat
			pointZ = false;
			assistZ = false;
			rebZ = false;
			blkZ = false;
			stealZ = false;
			
			// Iterating through all players and their traditional stats
			for (int j = 0; j < filteredTable.getModel().getColumnCount(); j++) {
				
				if (filteredTable.getColumnName(j).equals("PTS")) {
					double stat = Double.parseDouble
							(filteredTable.getModel().getValueAt(i, j).toString());
					if ((stat - pointMean) / pointDev < pointsThresh) {
						pointZ = true;
					}
				}
				if (filteredTable.getColumnName(j).equals("AST")) {
					double stat = Double.parseDouble
							(filteredTable.getModel().getValueAt(i, j).toString());
					if ((stat - assistMean) / assistDev < assistThresh) {
						assistZ = true;
					}
				}
				
				if (filteredTable.getColumnName(j).equals("BLK")) {
					double stat = Double.parseDouble
							(filteredTable.getModel().getValueAt(i, j).toString());
					if ((stat - blockMean) / blockDev < blksThresh) {
						blkZ = true;
					}
				}
				
				if (filteredTable.getColumnName(j).equals("STL")) {
					double stat = Double.parseDouble
							(filteredTable.getModel().getValueAt(i, j).toString());
					if ((stat - stealMean) / stealDev < stealsThresh) {
						stealZ = true;
					}
				}				
			}
			
			// If player is not in the top echelon for any of the traditional stats,
			// they cannot be an MVP caliber player and therefore must be filtered out
			if (pointZ && assistZ && blkZ && stealZ) {
					((DefaultTableModel)filteredTable.getModel()).removeRow(i);
					i--;
			}
		}

	    // Formatting filtered table by removing extra stats columns
		while(filteredTable.getColumnCount() > 30) {
	    	removeColumn(30, filteredTable, true, types);
	    }
		
		// Renaming of Player and Team name columns 
		removeColumn(1, filteredTable, true, types);
		removeColumn(1, filteredTable, true, types);

	    filteredTable.setRowHeight(50);
		panel.remove(js);
		
		// Reformatting the filtered table
        statsPicker(pruneType);
		js = new JScrollPane(filteredTable);
		filteredTable.getColumnModel().getColumn(0).setPreferredWidth(100);
	    filteredTable.getTableHeader().setFont(new Font("Arial", Font.PLAIN, 12));
	    filteredTable.setFont(new Font("Arial", Font.PLAIN, 12));
	    for (int i = 1; i < filteredTable.getColumnCount(); i++) {
	    	filteredTable.getColumnModel().getColumn(i).setPreferredWidth(60);
	    }
	    
	    // Setting constraints to format the panel
		GridBagConstraints constraint = new GridBagConstraints();
		constraint.weightx = 2.0;
	    constraint.weighty = 1.0;
	    constraint.fill = GridBagConstraints.BOTH;
	    constraint.gridx = 0;
	    constraint.gridy = 0;
	    constraint.gridwidth = 20;
	    constraint.gridheight= 3;
 		filteredTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	    panel.add(js, constraint);

		constraint.weightx = 2.0;
	    constraint.weighty = 1.0;
	    constraint.fill = GridBagConstraints.BOTH;
	    constraint.gridx = 2;
	    constraint.gridy = 20;
	    constraint.gridwidth = 20;
	    constraint.gridheight= 3;
	    
	    // Initializing the string function to print the function and weights
		String function = String.format( "    " + pruneType + " SCORE "
				+ "= %.2f*POINTS^%.2f + %.2f*ASSISTS^%.2f + %.2f*REBOUNDS^%.2f "
				+ "+ %.2f*BLOCKS^%.2f + %.2f*STEALS^%.2f + %.2f*WINS^%.2f", 
				pointsCoE, pointsExp, assistsCoE, assistsExp, 
				rebsCoE, rebsExp, blocksCoE, blocksExp, stealsCoE, stealsExp, winsCoE, winsExp);
		mvpFunction = new JLabel(function);
		
		panel.add(mvpFunction, constraint);
		panel.revalidate();
		panel.repaint();
		scoreCalculate(pruneType); 
	}
	
	// Implementing sliders to change the weight of each statistic in determining
	// the mvp score of each player and adding the sliders to the frame
	public void statsPicker(String scoreType) {
		// Creating a new panel for all sliders
		JPanel sliders = new JPanel(new GridLayout(5, 1));
		
		// Creating a new panel for this statistic
		JPanel pointsPanel = new JPanel(new GridLayout(4, 1));
		JSlider pointsS = new JSlider(0, 40);
		JSlider pointsP = new JSlider(0, 40);
		pointsS.setValue((int) (pointsCoE * 10));
		pointsP.setValue((int) (pointsExp * 10));
		JLabel  pointsSLabel = new JLabel(String.format("Points Coefficent: %.2f", pointsCoE));
		JLabel  pointsExpo = new JLabel(String.format("Points Exponent %.2f", pointsExp));
		
		// Linking slider to actions
		pointsS.addChangeListener(new ChangeListener() {
			 public void stateChanged(ChangeEvent e) {
				pointsCoE = (double) pointsS.getValue() / 10.0;
				pointsSLabel.setText(String.format("Points Coefficent %.2f", pointsCoE));
				scoreCalculate(scoreType);
				
			}
		});
		
		// Linking slider to actions
		pointsP.addChangeListener(new ChangeListener() {
			 public void stateChanged(ChangeEvent e) {
				pointsExp = (double) pointsP.getValue() / 10.0;
				pointsExpo.setText(String.format("Points Exponent %.2f", pointsExp));
				scoreCalculate(scoreType);
			}
		});
		
		// Setting slider bounds
		pointsS.setBounds(0, 0, 100, 10);
		pointsP.setBounds(0, 0, 100, 10);
		
		// Formatting slider panel
		pointsPanel.add(pointsSLabel);
		pointsPanel.add(pointsS);
		pointsPanel.add(pointsExpo);
		pointsPanel.add(pointsP);

		// Creating a new panel for this statistic
		JPanel  assistsPanel = new JPanel(new GridLayout(4, 1));
		JSlider astS = new JSlider(0, 40);
		JSlider astP = new JSlider(0, 40);
		astS.setValue((int) (assistsCoE * 10));
		astP.setValue((int)(assistsExp * 10));
		JLabel  astSLabel = new JLabel(String.format("Assists Coefficent: %.2f", assistsCoE));
		JLabel  astSExpo = new JLabel(String.format("Assists Exponent: %.2f", assistsExp));
		
		// Linking slider to actions
		astS.addChangeListener(new ChangeListener() {
			 public void stateChanged(ChangeEvent e) {
				assistsCoE = (double) astS.getValue() / 10.0;
				astSLabel.setText(String.format("Assists Coefficent: %.2f", assistsCoE));
				scoreCalculate(scoreType);
			}
		});
		
		// Linking slider to actions
		astP.addChangeListener(new ChangeListener() {
			 public void stateChanged(ChangeEvent e) {
				assistsExp = (double) astP.getValue() / 10.0;
				astSExpo.setText(String.format("Assists Exponent: %.2f", assistsExp));
				scoreCalculate(scoreType);
			}
		});
		
		// Setting slider bounds
		astS.setBounds(0, 0, 100, 10);
		astP.setBounds(0, 0, 100, 10);
		
		// Formatting slider panel
		assistsPanel.add(astSLabel);
		assistsPanel.add(astS);
		assistsPanel.add(astSExpo);
		assistsPanel.add(astP);
		
		// Creating a new panel for this statistic
		JPanel rebsPanel = new JPanel(new GridLayout(4, 1));
		JSlider rebS = new JSlider(0, 40);
		JSlider rebP = new JSlider(0, 40);
		rebS.setValue((int) (rebsCoE * 10));
		rebP.setValue((int) (rebsExp * 10));
		JLabel  rebSLabel = new JLabel(String.format("Rebounds Coefficent: %.2f", rebsCoE));
		JLabel  rebPExpo = new JLabel(String.format("Rebounds Exponent: %.2f", rebsExp));
		
		// Setting slider bounds
		rebS.setBounds(0, 0, 100, 10);
		rebP.setBounds(0, 0, 100, 10);
		
		// Formatting slider panel
		rebsPanel.add(rebSLabel);
		rebsPanel.add(rebS);
		rebsPanel.add(rebPExpo);
		rebsPanel.add(rebP);
		
		// Linking slider to actions
		rebS.addChangeListener(new ChangeListener() {
			 public void stateChanged(ChangeEvent e) {
				rebsCoE = (double) rebS.getValue() / 10.0;
				rebSLabel.setText(String.format("Rebounds Coefficent: %.2f", rebsCoE));
				scoreCalculate(scoreType);
			}
		});
		// Linking slider to actions
		rebP.addChangeListener(new ChangeListener() {
			 public void stateChanged(ChangeEvent e) {
				rebsExp = (double) rebP.getValue() / 10.0;
				rebPExpo.setText(String.format("Rebounds Exponent: %.2f", rebsExp));
				scoreCalculate(scoreType);
			}
		});
		
		// Creating a new panel for this statistic
		JPanel  blkPanel = new JPanel(new GridLayout(4, 1));		
		JSlider blkS = new JSlider(0, 40);
		JSlider blkP = new JSlider(0, 40);
		blkS.setValue((int) (blocksCoE * 10));
		blkP.setValue((int) (blocksExp * 10));
		JLabel  blkSLabel = new JLabel(String.format("Blocks Coefficent: %.2f", blocksCoE));
		JLabel  blkPLabel = new JLabel(String.format("Blocks Coefficent: %.2f", blocksExp));
		
		// Linking slider to actions
		blkS.addChangeListener(new ChangeListener() {
			 public void stateChanged(ChangeEvent e) {
				blocksCoE = (double) blkS.getValue() / 10.0;
				blkSLabel.setText(String.format("Blocks Coefficent: %.2f", blocksCoE));
				scoreCalculate(scoreType);
			}
		});
		// Linking slider to actions
		blkP.addChangeListener(new ChangeListener() {
			 public void stateChanged(ChangeEvent e) {
				blocksExp = (double) blkP.getValue() / 10.0;
				blkPLabel.setText(String.format("Blocks Coefficent: %.2f", blocksExp));
				scoreCalculate(scoreType);
			}
		});
		
		// Setting slider bounds
		blkS.setBounds(0, 0, 100, 10);
		blkP.setBounds(0, 0, 100, 10);
		
		// Formatting slider panel
		blkPanel.add(blkSLabel);
		blkPanel.add(blkS);
		blkPanel.add(blkPLabel);
		blkPanel.add(blkP);
		
		// Creating a new panel for this statistic
		JPanel  stlPanel = new JPanel(new GridLayout(4, 1));		
		JSlider stlS = new JSlider(0, 40);
		JSlider stlP = new JSlider(0, 40);
		stlS.setValue((int) (stealsCoE * 10));
		stlP.setValue((int) (stealsExp * 10));
		JLabel  stlSLabel = new JLabel(String.format("Steals Coefficent: %.2f", stealsCoE));
		JLabel  stlPLabel = new JLabel(String.format("Steals Exponent: %.2f", stealsExp));
		
		// Linking slider to actions
		stlS.addChangeListener(new ChangeListener() {
			 public void stateChanged(ChangeEvent e) {
				stealsCoE = (double) stlS.getValue() / 10.0;
				stlSLabel.setText(String.format("Steals Coefficent: %.2f", stealsCoE));
				scoreCalculate(scoreType);
			}
		});
		// Linking slider to actions
		stlP.addChangeListener(new ChangeListener() {
			 public void stateChanged(ChangeEvent e) {
				stealsExp = (double) stlP.getValue() / 10.0;
				stlPLabel.setText(String.format("Steals Exponent: %.2f", stealsExp));
				scoreCalculate(scoreType);
			}
		});
		
		// Setting slider bounds
		stlS.setBounds(0, 0, 100, 10);
		stlP.setBounds(0, 0, 100, 10);
		
		// Formatting slider panel
		stlPanel.add(stlSLabel);
		stlPanel.add(stlS);
		stlPanel.add(stlPLabel);
		stlPanel.add(stlP);
		
		// Creating a new panel for this statistic
		JPanel  winPanel = new JPanel(new GridLayout(4, 1));		
		JSlider winS = new JSlider(0, 40);
		JSlider winP = new JSlider(0, 40);
		winS.setValue((int) (winsCoE * 10));
		winP.setValue((int) (winsExp * 10));
		JLabel  winLabel = new JLabel(String.format("Wins Coefficent: %.2f", winsCoE));
		JLabel 	winPLabel = new JLabel(String.format("Wins Exponent: %.2f", winsExp));
		
		// Linking slider to actions
		winS.addChangeListener(new ChangeListener() {
			 public void stateChanged(ChangeEvent e) {
				winsCoE = (double) winS.getValue() / 10.0;
				winLabel.setText(String.format("Wins Coefficent: %.2f", winsCoE));
				scoreCalculate(scoreType);
			}
		});
		// Linking slider to actions
		winP.addChangeListener(new ChangeListener() {
			 public void stateChanged(ChangeEvent e) {
				winsExp = (double) winP.getValue() / 10.0;
				winPLabel.setText(String.format("Wins Exponent: %.2f", winsExp));
				scoreCalculate(scoreType);
			}
		});
		
		// Setting slider bounds
		winS.setBounds(0, 0, 100, 10);
		winP.setBounds(0, 0, 100, 10);
		
		// Formatting panel
		winPanel.add(winLabel);
		winPanel.add(winS);
		winPanel.add(winPLabel);
		winPanel.add(winP);
		
		// Adding each stat panel to the sliders panel
		sliders.add(pointsPanel);
		sliders.add(assistsPanel);
		sliders.add(rebsPanel);
		sliders.add(blkPanel);
		sliders.add(stlPanel);
		sliders.add(winPanel);
		
		// Formatting sliders panel through constraints
		GridBagConstraints constraint = new GridBagConstraints();
	    constraint.weightx = .2;
	    constraint.weighty = .1;
	    constraint.gridx = 20;
	    constraint.gridy = 0;
	    constraint.gridwidth = 20;
	    constraint.gridheight= 3;
		constraint.fill = GridBagConstraints.NONE;
		// Adding sliders panel and constraints to full panel
	    panel.add(sliders, constraint);
		panel.repaint();
		panel.revalidate();
	}
	
	boolean mvpColSet = true;
	
	// Calculating mvp score
	public void scoreCalculate(String scoreType) {
		
		// Creating string to display function used to calculate mvp score
		String function = "    "  + scoreType + String.format(" SCORE = %.2f*POINTS^%.2f + %.2f*ASSISTS^%.2f + %.2f*REBOUNDS^%.2f + %.2f*BLOCKS^%.2f + %.2f*STEALS^%.2f + %.2f*WINS^%.2f", 
				pointsCoE, pointsExp, assistsCoE, assistsExp, rebsCoE, rebsExp, blocksCoE, blocksExp, stealsCoE, stealsExp, winsCoE, winsExp);
			
		DefaultTableModel model = (DefaultTableModel) filteredTable.getModel();
		
		// Adding "MVP Score" column to the table if not yet added
		if (mvpColSet) {
			mvpColSet = false;
			model.addColumn(scoreType + " Score");
			filteredTable.moveColumn(model.getColumnCount() - 1, 1);
			TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) filteredTable.getRowSorter();
			sorter.toggleSortOrder(model.getColumnCount() -1);
			sorter.toggleSortOrder(model.getColumnCount() -1);
			filteredTable.setRowSorter(sorter);
		}
		
		mvpFunction.setText(function);
		
		// Obtaining stats for each player and calculating mvp scores
		for (int i = 0; i < model.getRowCount(); i++) {
			String playerName = model.getValueAt(i, 0).toString();
			JSONObject player = playerMap.get(playerName);

			double playerPoints = 0;
			double playerRebs  = 0;
			double playerAssits  = 0;
			double playerBlocks  = 0;
			double playerSteals  = 0;
			double playerWins  = 0;
			int playerGP = 0;
			
			try {
				// Obtaining player stats
				playerPoints = (double) player.getDouble("PTS");
				playerAssits = (double) player.getDouble("AST");
				playerRebs = (double) player.getDouble("REB");
				playerBlocks = (double) player.getDouble("BLK");
				playerSteals = (double) player.getDouble("STL");
				playerWins = (double) player.getInt("W");
				playerWins = (double) player.getInt("W");
				playerGP = (int) player.getInt("GP");
			} catch (JSONException e) {
				
			}
			// Calculating values for each statistic depending on the weights
			double pointsVal = pointsCoE * Math.pow(playerPoints, pointsExp);
			double assistsVal = assistsCoE * Math.pow(playerRebs, assistsExp);
			double rebsVal = rebsCoE * Math.pow(playerAssits, rebsExp);
			double blocksVal = blocksCoE * Math.pow(playerBlocks, blocksExp);
			double stealsVal = stealsCoE * Math.pow(playerSteals, stealsExp);
			double winsVal = playerGP/72.0 * winsCoE * Math.pow(playerWins, winsExp);
			
			// Getting mvp score and adding score to the table
			double mvpScore = pointsVal + assistsVal + rebsVal + blocksVal + stealsVal + winsVal;
			model.setValueAt(mvpScore, i, model.getColumnCount() - 1);
		}
		// Repaint panel to reflect changes
		model.fireTableDataChanged();
		panel.repaint();
		panel.revalidate();
	}	

	public static void main(String[] args) {
	    nbaStats test = new nbaStats();
	    test.displayTable(); 
	}
}



