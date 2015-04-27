package pkg.trader;

import java.util.ArrayList;

import pkg.exception.StockMarketExpection;
import pkg.market.Market;
import pkg.order.BuyOrder;
import pkg.order.Order;
import pkg.order.OrderType;
import pkg.order.SellOrder;
import pkg.util.OrderUtility;

public class Trader {
	String name;
	double cashInHand;
	ArrayList<Order> stocksOwned;
	ArrayList<Order> ordersPlaced;

	public Trader(String name, double cashInHand) {
		super();
		this.name = name;
		this.cashInHand = cashInHand;
		this.stocksOwned = new ArrayList<Order>();
		this.ordersPlaced = new ArrayList<Order>();
	}

	public void buyFromBank(Market m, String symbol, int volume)
			throws StockMarketExpection {
		
		//Get total stock price
		double price = m.getStockForSymbol(symbol).getPrice();
		
		//Check if trader has enough cash
		if (price * volume > cashInHand) {
			throw new StockMarketExpection(this.name + " cannot afford to buy this stock");
		}
		
		Order order = new BuyOrder(symbol, volume, price , this);
		this.stocksOwned.add(order);
		this.cashInHand -= price * volume;

	}

	public void placeNewOrder(Market market, String symbol, int volume,
			double price, OrderType orderType) throws StockMarketExpection {
		// Place a new order and add to the orderlist
		// Also enter the order into the orderbook of the market.
		// Note that no trade has been made yet. The order is in suspension
		// until a trade is triggered.
		//
		// If the stock's price is larger than the cash possessed, then an
		// exception is thrown

		
		double total = price * volume;
		

		//Check if trader has enough cash to buy stock
		if (orderType == OrderType.BUY && total > cashInHand) {

			throw new StockMarketExpection(this.name + " cannot afford to buy this stock");
		}

		Order order = null;
		if (orderType == OrderType.BUY) {
			order = new BuyOrder(symbol, volume, price, this);
		}
		else {
			order = new SellOrder(symbol, volume, price, this);
		}

		//Check for multiple stock orders
		if (OrderUtility.isAlreadyPresent(ordersPlaced, order)) {
			throw new StockMarketExpection("There is already an order for this stock");
		}

		// Check that trader owns enough of stock being sold
		if (orderType == OrderType.SELL && volume > OrderUtility.ownedQuantity(this.stocksOwned, symbol)) {
			throw new StockMarketExpection("Sell order volume is larger than amount of stock owned");
		}

		// Check that trader owns the stock
		if (orderType == OrderType.SELL && !OrderUtility.owns(stocksOwned, symbol)) {
			throw new StockMarketExpection(this.name + " cannot place a sell order for a stock not owned");
		}
		
		market.addOrder(order);
		this.ordersPlaced.add(order);


	}

	public void placeNewMarketOrder(Market market, String symbol, int volume,
			double price, OrderType orderType) throws StockMarketExpection {
	
		// Similar to the other method, except the order is a market order

		double total = market.getStockForSymbol(symbol).getPrice() * volume;

		//Check if trader has enough cash to buy stock
		if (total > cashInHand) {
			throw new StockMarketExpection(this.name + " cannot afford to buy this stock");
		}

		Order order = null;
		if (orderType == OrderType.BUY) {
			order = new BuyOrder(symbol, volume, true, this);
		}
		else {
			order = new SellOrder(symbol, volume, true, this);
		}

		//Check for multiple stock orders
		if (OrderUtility.isAlreadyPresent(ordersPlaced, order)) {
			throw new StockMarketExpection("There is already an order for this stock");
		}

		// Check that trader owns enough of stock being sold
		if (orderType == OrderType.SELL && volume > OrderUtility.ownedQuantity(this.stocksOwned, symbol)) {
			throw new StockMarketExpection("Sell order volume is larger than amount of stock owned");
		}

		// Check that trader owns the stock
		if (orderType == OrderType.SELL && !OrderUtility.owns(stocksOwned, symbol)) {
			throw new StockMarketExpection(this.name + " cannot place a sell order for a stock not owned");
		}
		
		// Enter order into orderbook
		market.addOrder(order);
		this.ordersPlaced.add(order);

	}

	public void tradePerformed(Order order, double matchPrice)
			throws StockMarketExpection {
		// Notification received that a trade has been made, the parameters are
		// the order corresponding to the trade, and the match price calculated
		// in the order book. 


		if (!OrderUtility.isAlreadyPresent(ordersPlaced, order)) {
			throw new StockMarketExpection("Order does not exist in ordersPlaced");
		}
		
		if (SellOrder.class.isInstance(order)) {		
			this.cashInHand += matchPrice * order.getSize();
			OrderUtility.findAndExtractOrder(stocksOwned, order.getStockSymbol());
			
		} else if (BuyOrder.class.isInstance(order)) {
			this.cashInHand -= matchPrice * order.getSize(); 
			this.stocksOwned.add(order); 

		}
		this.ordersPlaced.remove(order);

	}


	public void printTrader() {
		System.out.println("Trader Name: " + name);
		System.out.println("=====================");
		System.out.println("Cash: " + cashInHand);
		System.out.println("Stocks Owned: ");
		for (Order o : stocksOwned) {
			o.printStockNameInOrder();
		}
		System.out.println("Stocks Desired: ");
		for (Order o : ordersPlaced) {
			o.printOrder();
		}
		System.out.println("+++++++++++++++++++++");
		System.out.println("+++++++++++++++++++++");
	}
}
