package pkg.order;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.HashMap;
import pkg.exception.StockMarketExpection;
import pkg.market.Market;
import pkg.market.api.PriceSetter;

public class OrderBook {
	Market m;
	PriceSetter ps;
	HashMap<String, ArrayList<Order>> buyOrders;
	HashMap<String, ArrayList<Order>> sellOrders;
	double matchingPrice;
	//HashMap<Double, Integer> cumulativeBuyOrders; //price, total order volume for price
	//HashMap<Double, Integer> cumulativeSellOrders; //price, total order volume for price

	//int[] cumulativeBuyPerPrice;
	//int[] cumulativeSellPerPrice;

	public OrderBook(Market m) {
		this.m = m;
		buyOrders = new HashMap<String, ArrayList<Order>>();
		sellOrders = new HashMap<String, ArrayList<Order>>();

	}

	public void addToOrderBook(Order order) {
		// Populate the buyOrders and sellOrders data structures, whichever
		// appropriate
		if (order instanceof BuyOrder) {
			ArrayList<Order> orders;
			if (buyOrders.containsKey(order.getStockSymbol())) {
				orders = buyOrders.get(order.getStockSymbol());
			} 
			else { // create
				orders = new ArrayList<Order>();
			}
			
			orders.add(order);
			buyOrders.put(order.getStockSymbol(), orders);
		} else {

			ArrayList<Order> orders;
			if (sellOrders.containsKey(order.getStockSymbol())) {
				orders = sellOrders.get(order.getStockSymbol());
			} else { // create
				orders = new ArrayList<Order>();
			}
			
			orders.add(order);
			sellOrders.put(order.getStockSymbol(), orders);
		}
		
	}

	public void trade() {
		// Complete the trading.
		// 1. Follow and create the orderbook data representation (see spec)
		// 2. Find the matching price
		// 3. Update the stocks price in the market using the PriceSetter.
		// Note that PriceSetter follows the Observer pattern. Use the pattern.
		// 4. Remove the traded orders from the orderbook
		// 5. Delegate to trader that the trade has been made, so that the
		// trader's orders can be placed to his possession (a trader's position
		// is the stocks he owns)
		// (Add other methods as necessary)
		for (String stock : sellOrders.keySet()) {
			if (buyOrders.containsKey(stock)) {
				ArrayList<Order> sell = sellOrders.get(stock);
				ArrayList<Order> buy = buyOrders.get(stock);
				ArrayList<Order> orders;
				
				TreeMap<Double, ArrayList<Order>> sorted = new TreeMap<Double, ArrayList<Order>>();
				
				for (Order buyOrder : buy) { //populate buys
					if (sorted.containsKey(buyOrder.getPrice())) {
						orders = sorted.get(buyOrder.getPrice());
					} else { //create
						orders = new ArrayList<Order>();
					}
					orders.add(buyOrder);
					sorted.put(buyOrder.getPrice(), orders);
				}
				for (Order sellOrder : sell) { // populate sells
					if (sorted.containsKey(sellOrder.getPrice())) {
						orders = sorted.get(sellOrder.getPrice());
					} else { // create
						orders = new ArrayList<Order>();
					}
					orders.add(sellOrder);
					sorted.put(sellOrder.getPrice(), orders);
					
				}
				
				// handle and remove market orders
				double marketPrice = m.getStockForSymbol(stock).getPrice();
				ArrayList<Order> marketOrders = sorted.remove(0.0);
				int totalBuys = 0, totalSells = 0;
				
				if (marketOrders != null) {
				for (Order marketOrder : marketOrders) {
					if (marketOrder instanceof BuyOrder) {
						totalBuys += marketOrder.getSize();
					} 
					
					else {
						totalSells += marketOrder.getSize();
					}
				}
				}
				
				// cumulative prices structures
				int numPrices = sorted.size();
				int[] cumulativeLeastBuys = new int[numPrices];
				int[] cumulativeLeastSells = new int[numPrices];
				
				
				int i = 0;
				for (double price : sorted.keySet()) { //add up sell orders 
					ArrayList<Order> match = sorted.get(price);

					for (Order o : match) {
						if (o instanceof SellOrder) {
							totalSells += o.getSize();
						}
					}
					
					cumulativeLeastSells[i] = totalSells;
					i++;
				}
				
				int j = numPrices - 1;
				for (double price : sorted.descendingKeySet()) { //add up buy orders
					ArrayList<Order> match = sorted.get(price);

					for (Order o : match) {
						if (o instanceof BuyOrder) {
							totalBuys += o.getSize();
						}
					}
					
					cumulativeLeastBuys[j] = totalBuys;
					j--;
				}
				
				//calculating matching price
				int difference = Integer.MAX_VALUE;
				int k = 0;
				int matchingIndex = -1;
				matchingPrice = marketPrice;
				
				while (difference > 0 && k < numPrices) {
					int temp = cumulativeLeastBuys[k] - cumulativeLeastSells[k];
					if (temp < difference) {
						difference = temp;
						if (temp >= 0) {
							matchingIndex = k;
						}
					}
					k++;
				}
				for (double price : sorted.keySet()) {
					if (matchingIndex == 0) {
						matchingPrice = price;
						break;
					}
					matchingIndex--;
				}

				//Update the stocks price in the market using the PriceSetter
				PriceSetter priceSetter = new PriceSetter();
				priceSetter.registerObserver(m.getMarketHistory());
				m.getMarketHistory().setSubject(priceSetter);
				
				if (matchingPrice != marketPrice) {
					priceSetter.setNewPrice(m, stock, matchingPrice); //add
				}
				
				// 4. Remove the traded orders from the orderbook
				// 5. Delegate to trader that the trade has been made, so that the
				// trader's orders can be placed to his possession (a trader's position
				// is the stocks he owns)
				if (marketOrders != null) {
				for (Order marketOrder : marketOrders) { // remove market trades
					if (marketOrder instanceof BuyOrder) {
						buyOrders.get(stock).remove(marketOrder);
					} else {
						sellOrders.get(stock).remove(marketOrder);
					}
					
					try { // delegate to trader
						marketOrder.getTrader().tradePerformed(marketOrder, matchingPrice);
					} 
					catch (StockMarketExpection e) {
						e.printStackTrace();
					}
				}
				}
				
				for (double price : sorted.keySet()) { 
					ArrayList<Order> match = sorted.get(price);

					for (Order o : match) {
						if (o instanceof BuyOrder && price >= matchingPrice) {
							buyOrders.get(stock).remove(o);
							
							try { // delegate to trader
								o.getTrader().tradePerformed(o, matchingPrice);
							} 
							catch (StockMarketExpection e) {
								e.printStackTrace();
							}
						} else if (o instanceof SellOrder && price <= matchingPrice) {
							sellOrders.get(stock).remove(o);
							
							try { // delegate to trader
								o.getTrader().tradePerformed(o, matchingPrice);
							} 
							catch (StockMarketExpection e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
	}

}
