
private List<IOrder> getOrders() throws JFException {
    IOrder o;
    List<IOrder> accOrders = engine.getOrders(); //all active orders of the given account	
    
    //filter strategy orders
    List<IOrder> strOrders = new ArrayList<IOrder>();
    for(int i=0; i<accOrders.size(); i++) {
		o = accOrders.get(i);
    
        if(!o.getLabel().toUpperCase().startsWith(getStrategyName().toUpperCase())) {
            continue;
        }
        
        strOrders.add(o);
    }

	return strOrders;
}

//------------------------------------------------------------------


private void print(Object o){
	context.getConsole().getOut().println(getStrategyName()+" - "+o);
}

//------------------------------------------------------------------

private void print(Object ...data ) {
	context.getConsole().getOut().print(getStrategyName()+" - ");
	
	for(Object o : data) {
		context.getConsole().getOut().print(o+", ");
	}
	
	context.getConsole().getOut().println("");
}

//------------------------------------------------------------------

private void printBarStartTime(Instrument instrument, Period period) throws JFException {
	long startTime = history.getStartTimeOfCurrentBar(instrument, period);
	context.getConsole().getOut().println(SQTime.toFullDateMinuteString(startTime));
}

//------------------------------------------------------------------

private IOrder sqOpenOrder(OrderCommand orderType, String symbol, double size, double price, int magicNumber, String comment, long expirationInTime, boolean replaceExisting, boolean allowDuplicateTrades, Color arrowColor, boolean isExitLevel) throws JFException {
	if(size <= 0) return null;
	
	print("Opening order type " + orderType + " with price " + price);
	
	String correctedSymbol = correctSymbol(symbol);
	instrument = getInstrument(correctedSymbol);
	
	double ask = sqGetAsk(symbol);
	double bid = sqGetBid(symbol);

	int direction = sqGetDirectionFromOrderType(orderType);

	double marketPrice = direction == 1 ? ask : bid;

	price = price > 0 ? price : marketPrice;                
	price = sqFixMarketPrice(price, instrument);


   if(LimitMaxDistanceFromMarketPrice && (orderType != BUY && orderType != SELL)){
      double pctDistance = round(Math.abs(price-marketPrice) / marketPrice * 100, 2);
      if(pctDistance > round(MaxDistanceFromMarketPct, 2)){
         print("Order skipped - too far from market price. Open price: ", price, "Market price: ", marketPrice, "Max distance: ", MaxDistanceFromMarketPct + "%"); 
         return null;  
      }
   }

	//openingOrdersAllowed = openingOrdersAllowed && sqHandleTradingOptions();
	if(!openingOrdersAllowed)  return null;

	amount = sizeToAmount(size, instrument);
	
	print("Opening order type " + orderType + " with price " + price + ", size "+size+", amount "+amount+". Current market prices: " + ask + " / " + bid);

	// check if live order exists
	order = sqGetOrder(magicNumber, correctedSymbol, 0, comment, true, true, false);
	if(!isExitLevel && order!=null && !allowDuplicateTrades) {
	  print("Order with these parameters already exists and duplicate trades are not allowed. Canceling order...");
	  print("----------------------------------");
	  return null;
	}

	// check if pending order exists
	if(!isExitLevel){
		order = sqGetOrder(magicNumber, correctedSymbol, direction, comment, false, false, true);
		if(order!=null) {
		  if(replaceExisting) {
			 if(amount == order.getAmount()) {
				// modify existing pending order
				if(sqOrderModify(order, price, 0, 0, expirationInTime)) {
				   // reset global variables for this order
				   sqResetGlobalVariablesForTicket(order.getLabel());
				   return order;
				
				} else {
				   print("Modifying order failed, deleting it");
				   sqDeletePendingOrder(order, "Open order - failed to modify existing pending order");
				   return null;
				}
			 } else {
				// delete existing pending order
				sqDeletePendingOrder(order, "Open order - deleting existing pending order");
			 }

		  } else {
			 print("Pending Order with these parameters already exists, and replace is not allowed. Canceling order...");
			 return null;
		  }
		}
	}

	if(!checkOrderPriceValid(orderType, correctedSymbol, price, marketPrice)){
	  return null;
	}

	String commentToUse = "";
	if(!comment.isEmpty()){
	  commentToUse = comment;
	} else {
	  commentToUse = CustomComment;
	  commentToUse = commentToUse.replaceAll("Optimization", "Opt.");     //shorten the name of optimized strategies
	}
	
	if(commentToUse.length() > 30) {
		commentToUse = commentToUse.substring(0, 30);           //limit the length to 30 characters
	}
	
	String label = getLabel(instrument, magicNumber);
		
	order = engine.submitOrder(label, instrument, orderType, amount, price, correctSlippage(sqMaxEntrySlippage, correctedSymbol), 0, 0, expirationInTime, commentToUse);
	if(order != null) {
	  print("Opened order with ticket " + label + " ----------------");	
	  // reset global variables for this order
	  sqResetGlobalVariablesForTicket(order.getLabel());
	}

	return order;
}

//----------------------------------------------------------------------------

private boolean sqCheckConnected() {
	return true;
}

//----------------------------------------------------------------------------

private boolean checkOrderPriceValid(OrderCommand orderType, String symbol, double price, double marketPrice){
   if(orderType == BUY || orderType == SELL){
      if(marketPrice == price){
         return true;
      }
      else {
         print("Based on its logic, the strategy tried to place order a market order at incorrect price. Market price: " + marketPrice + ", order price: " + price + " (this is NOT an error)");
         return false;
      }
   }
   
   return checkStopPriceValid(orderType, symbol, price, marketPrice, "stop/limit order");
}

//----------------------------------------------------------------------------

private boolean checkStopPriceValid(OrderCommand orderType, String symbol, double price, double marketPrice, String name){
   if(orderType == BUYLIMIT || orderType == SELLSTOP){      
      if(price <= marketPrice){
         return true;
      }
      else {
         print("Based on its logic, the strategy tried to place ", name, " at incorrect price. Market price: ", marketPrice, " max. price allowed: ", marketPrice, name, " price: ", price, " (this is NOT an error)");
         return false;
      }
   }
   else if(orderType == BUYSTOP || orderType == SELLLIMIT){      
      if(price >= marketPrice){
         return true;
      }
      else {
         print("Based on its logic, the strategy tried to place ", name, " at incorrect price. Market price: ", marketPrice, " min. price allowed: ", marketPrice, name," price: ", price, " (this is NOT an error)");
         return false;
      }
   }
   else return true;
}  

//----------------------------------------------------------------------------

private boolean sqDeletePendingOrder(IOrder order, String reason) throws JFException {
	print("Deleting pending order, ticket: " + order.getLabel() + ", reason: " + reason);

	orderType = order.getOrderCommand();

	if(!orderType.isConditional()) {
		print("Trying to delete non-pending order");
		return false;
	}
	if(!sqCheckConnected()) {
		return false;
	}
	 
	if(order.getState() == FILLED || order.getState() == OPENED){
		order.close();
		print("Order deleted successfuly");
		return true;
	} else {
		print("Unable to close order in state: " + order.getState());
		return false;
	}
}

//+------------------------------------------------------------------+

private boolean sqOrderModifySL(IOrder order, double stopLoss, int type) throws JFException {
   if(type == SLPTTYPE_RANGE) {
      // convert range to price level
      if(sqGetDirectionFromOrderType(order.getOrderCommand()) == 1) {
         // it is long order
         stopLoss = order.getOpenPrice() - stopLoss;
      } else {
         stopLoss = order.getOpenPrice() + stopLoss;
      }
   }

   return(sqOrderModify(order, order.getOpenPrice(), sqFixMarketPrice(stopLoss, order.getInstrument()), order.getTakeProfitPrice()));
}

//+------------------------------------------------------------------+

private boolean sqOrderModifyPT(IOrder order, double profitTarget, int type) throws JFException {
   if(type == SLPTTYPE_RANGE) {
      // convert range to price level
      if(sqGetDirectionFromOrderType(order.getOrderCommand()) == 1) {
         // it is long order
         profitTarget = order.getOpenPrice() + profitTarget;
      } else {
         profitTarget = order.getOpenPrice() - profitTarget;
      }
   }

   return(sqOrderModify(order, order.getOpenPrice(), order.getStopLossPrice(), sqFixMarketPrice(profitTarget, order.getInstrument())));
}

//+------------------------------------------------------------------+

//----------------------------------------------------------------------------

private boolean sqOrderModify(IOrder order, double price, double stopLoss, double profitTarget) throws JFException {
	return  sqOrderModify(order, price, stopLoss, profitTarget, -1);
}

//----------------------------------------------------------------------------

private boolean sqOrderModify(IOrder order, double price, double stopLoss, double profitTarget, long expirationInTime) throws JFException {
    try {
        print("Modifying order with ticket " + order.getLabel() + ", openPrice: " + price + ", sl: " + stopLoss + ", pt: " + profitTarget + ", state: " + order.getState() + ", conditional: " + order.getOrderCommand().isConditional());
    
        String changes = "";
    
        if (order.getState() == IOrder.State.CREATED) {
            order.waitForUpdate(5000);
        }
        
        if (!order.getOrderCommand().isConditional() && order.getState() == IOrder.State.OPENED) { //Simple BUY or SELL orders can have this state before filling or can get FILLED state right after CREATED state
            order.waitForUpdate(5000);
        }
        
        print("Order state before updating the order ", order.getLabel(), order.getState());
        
        if(price > 0 && order.getOpenPrice() != price && order.getOrderCommand().isConditional()) {
			print("Modifying order open price " + order.getLabel() + ", "+order.getOpenPrice()+" -> "+price);
			
			//sets the open price of pending order (in OPENED state or pending part of partially filled order)
			if (order.getState() == IOrder.State.OPENED || (order.getState() == IOrder.State.FILLED && order.getAmount() < order.getRequestedAmount())) {
				order.setOpenPrice(price);
							
				changes += ", open price: " + price;
			} else { //order has been completely filled
				print("Cannot modify order open price, not in OPENED or FILLED(partially) state" + order.getLabel(), order.getState());
			}
        }    
        
        if(stopLoss > 0 && order.getStopLossPrice() != stopLoss)         {
            print("Modifying order SL " + order.getLabel() + ", "+ order.getStopLossPrice()+" -> "+stopLoss);
            
            order.setStopLossPrice(stopLoss);

            changes += ", SL: " + stopLoss;
        }
        
        if(profitTarget > 0 && order.getTakeProfitPrice() != profitTarget) {
            print("Modifying order PT " + order.getLabel() + ", "+order.getTakeProfitPrice()+" -> "+profitTarget);
            
            order.setTakeProfitPrice(profitTarget);
                        
            changes += ", PT: " + profitTarget;
        }
    
        if(expirationInTime > 0) {
            print("Modifying order expirationInTime " + order.getLabel() + ", "+order.getGoodTillTime()+" -> "+expirationInTime);
            
            order.setGoodTillTime(expirationInTime);
                        
            changes += ", expiration time: " + expirationInTime;
        }
        
        print("Modified order parameters " + order.getLabel() + changes);
        
        return true;
    } catch(Exception e) {
        throw new JFException("Error!!! Cannot modify order " + order.getLabel() + e.getMessage(), e);
        
    } catch(Error ex) {
        throw new Error("Error!!! Cannot modify order " + order.getLabel() + ex.getMessage(), ex);
    }
}

//----------------------------------------------------------------------------

private int correctSlippage(int slippage, String symbol){
    if(slippage <= 0) return 100000;
    
    return slippage;
}

//------------------------------------------------------------------

private String correctSymbol(String symbol){
  if(symbol.equals("NULL") || symbol.equals("Current") || symbol.equals("Same as main chart")) {
      return symbol;
  }
  <@jfPrintSubchartConditions />
  else return symbol;
}

//------------------------------------------------------------------

private double sqGetAsk(String symbol) throws JFException {
	if(symbol.equals("NULL") || symbol.equals("Current")) {
		instrument = selectedInstrument;
	} else {
		instrument = getInstrument(correctSymbol(symbol));
	}	
   
	return sqGetAsk(instrument);
}

//------------------------------------------------------------------

private double sqGetAsk(Instrument instrument) throws JFException {
	return history.getLastTick(instrument).getAsk();
}

//------------------------------------------------------------------

private double sqGetBid(String symbol) throws JFException {
	if(symbol.equals("NULL") || symbol.equals("Current")) {
		instrument = selectedInstrument;
	} else {
		instrument = getInstrument(correctSymbol(symbol));
	}	
   
	return sqGetBid(instrument);
}

//------------------------------------------------------------------

private double sqGetBid(Instrument instrument) throws JFException {
	return history.getLastTick(instrument).getBid();
}


//------------------------------------------------------------------

private void sqClosePosition(double size, int magicNumber, String symbol, int direction, String comment, String reason) throws JFException {
   print("Closing order with Magic Number: " + magicNumber + ", symbol: " + symbol + ", direction: " + direction + ", comment: " + comment + ", reason: " + reason);

   order = sqGetOrder(magicNumber, symbol, direction, comment, false, false, false);

   if(order == null) {
      print("Order cannot be found");
   } else {
	  orderType = order.getOrderCommand();
	   
      if(!orderType.isConditional()) {
         sqClosePositionAtMarket(order, size, reason);
      } else {
         sqDeletePendingOrder(order, reason);
      }
   }

   print("Closing order finished ----------------");
}

//------------------------------------------------------------------

private boolean sqClosePositionAtMarket(IOrder order, double size, String reason) throws JFException {
	print("Closing order with ticket: " + order.getLabel() + ", reason: " + reason);

	orderType = order.getOrderCommand();

	if(orderType.isConditional()) {
      print("Trying to close non-live order");
      return false;
	}
   
	if(!sqCheckConnected()) {
      return false;
	}

	if(size > 0) {
		order.close(size / 10);
	} else {
		order.close(); //with size 0 it throws an exception
	}
	
	print("Order closed successfuly");
	
	return true;
}

//------------------------------------------------------------------

private void sqClosePendingOrder(int magicNumber, String symbol, int direction, String comment) throws JFException {
   print("Closing pending order with Magic Number: ", magicNumber, ", symbol: ", symbol, ", direction: ", direction, ", comment: ", comment);

   order = sqGetOrder(magicNumber, symbol, direction, comment, false, false, true);
   if(order==null) {
      print("Order cannot be found");
   } else {
      sqDeletePendingOrder(order, "Close pending order");
   }

   print("Closing pending order finished ----------------");
}

//------------------------------------------------------------------

private void sqCloseAllPendingOrders(int magicNumber, String symbol, int direction, String comment) throws JFException {
   print("Closing pending orders with Magic Number: ", magicNumber, ", symbol: ", symbol, ", direction: ", direction, ", comment: ", comment);

   while(true) {
	  order = sqGetOrder(magicNumber, symbol, direction, comment, false, false, true);
	  if(order==null) break;
	  
      sqDeletePendingOrder(order, "Close all pending orders");
   }

   print("Closing pending orders finished ----------------");
}

//------------------------------------------------------------------

private OrderCommand getOrderType(int orderType) {
	   switch(orderType) {
			 case 0: return BUY;
			 case 1: return SELL;
			 case 2: return BUYLIMIT;
			 case 3: return SELLLIMIT;
			 case 4: return BUYSTOP;
			 case 5: return SELLSTOP;
	   }
	   
	   return null;
}

//------------------------------------------------------------------

private IOrder  sqGetPendingOrderByType(int magicNumber, String symbol, int orderType, String comment) throws JFException {
   orders = getOrders();	

   OrderCommand orderCommand = getOrderType(orderType);

   for (int cc = 0; cc < orders.size(); cc++) {
		 order = orders.get(cc);
		 
		 if(!order.getOrderCommand().isConditional()) {
			continue;
		 }
         
         if(orderType != 0) {
            if(orderCommand != order.getOrderCommand()) continue;
         }

         if(magicNumber != 0) {
            if(!checkOrderMagicNumber(order, magicNumber)) continue;
         }  
         else if(!checkMagicNumber(getOrderMagicNumber(order))) continue;

         if(!symbol.equals("Any")) {
            if(order.getInstrument() != getInstrument(symbol)) continue;
         }

         if(!comment.isEmpty()) {
            if(order.getComment().indexOf(comment) < 0) continue;
         }

         // otherwise we found the order
         return order;
   }

   return null;
}

//------------------------------------------------------------------

private IOrder sqGetPendingOrderByDir(int magicNumber, String symbol, int direction, String comment) throws JFException {
   orders = getOrders();	

   for (int cc = 0; cc < orders.size(); cc++) {
		 order = orders.get(cc);
		 orderType = order.getOrderCommand();
		 
		 if(!orderType.isConditional()) {
			continue;
		 }
	   
         if(direction != 0) {
            int orderDirection = sqGetDirectionFromOrderType(orderType);
            if(orderDirection != direction) continue;
         }

         if(magicNumber != 0) {
            if(!checkOrderMagicNumber(order, magicNumber)) continue;
         }  
         else if(!checkMagicNumber(getOrderMagicNumber(order))) continue;

         if(!symbol.equals("Any")) {
            if(order.getInstrument() != getInstrument(symbol)) continue;
         }

         if(!comment.isEmpty()) {
            if(order.getComment().indexOf(comment) < 0) continue;
         }

		 // otherwise we found the order
		 return order;
   }

   return null;
}

//------------------------------------------------------------------
//bool sqSelectOrder(int magicNumber, String symbol, int direction, String comment, bool goFromNewest=true, bool skipPending=true, bool skipFilled=false) {
private IOrder sqGetOrder(int magicNumber, String symbol, int direction, String comment, boolean goFromNewest, boolean skipPending, boolean skipFilled) throws JFException {
	instrument = getInstrument(symbol);
	    
	orders = getOrders();	
		
    if(goFromNewest){
       for (int cc = orders.size() - 1; cc >= 0; cc--) {
		   order = orders.get(cc);
		   
           if(orderFits(order, magicNumber, symbol, direction, comment, skipPending, skipFilled)){
              return order;
           }
       }
    }
    else {
       for (int cc = 0; cc < orders.size(); cc++) {
		   order = orders.get(cc);
		   
           if(orderFits(order, magicNumber, symbol, direction, comment, skipPending, skipFilled)){
              return order;
           }
       }
    }
	
    return null;
}

//------------------------------------------------------------------

private IOrder sqGetOrder(int magicNumber, String symbol, int direction, String comment, boolean goFromNewest, boolean skipPending) throws JFException {
	return sqGetOrder(magicNumber, symbol, direction, comment, goFromNewest, skipPending, false);
}

//------------------------------------------------------------------

private IOrder sqGetOrder(int magicNumber, String symbol, int direction, String comment, boolean goFromNewest) throws JFException {
	return sqGetOrder(magicNumber, symbol, direction, comment, goFromNewest, true, false);
}

//------------------------------------------------------------------

private IOrder sqGetOrder(int magicNumber, String symbol, int direction, String comment) throws JFException {
	return sqGetOrder(magicNumber, symbol, direction, comment, true, true, false);
}

//------------------------------------------------------------------

private boolean orderFits(IOrder order, int magicNumber, String symbol, int direction, String comment, boolean skipPending, boolean skipFilled){
	orderType = order.getOrderCommand();

	// skip pending orders
	if(skipPending && (orderType.isConditional())) {
		return false;
	}
	
	// skip filled orders
	if(skipFilled && (!orderType.isConditional())) {
		return false;
	}

	if(direction != 0) {
		if(direction > 0 && (!orderType.isLong())) return false;
		if(direction < 0 && (!orderType.isShort())) return false;
	}

	if(magicNumber != 0) {
		if(!checkMagicNumber(magicNumber) || !checkOrderMagicNumber(order, magicNumber)) return false;
	}

	if(!symbol.equals("Any")) {
		if(order.getInstrument() != getInstrument(symbol)) return false;
	}

	if(!comment.isEmpty()) {
		if(order.getComment().indexOf(comment) < 0) return false;
	}

	// otherwise we found the order
	return true;
}

//------------------------------------------------------------------

private boolean checkOrderMagicNumber(IOrder order, int magicNumber) {
	return order.getLabel().endsWith("_"+magicNumber);
}

//------------------------------------------------------------------

private int getOrderMagicNumber(IOrder order) {
	String label = order.getLabel();
	return Integer.valueOf(label.substring(label.lastIndexOf("_") + 1));
}

//------------------------------------------------------------------

private void sqManageOrders() throws JFException {
	orders = getOrders();	
	
	for (int cc = orders.size() - 1; cc >= 0; cc--) {
		order = orders.get(cc);
		orderType = order.getOrderCommand();
		
		String label = order.getLabel();

		if(!orderType.isConditional()) {
			// handle Exit Methods only for live orders
			sqManageSL2BE(order);
			sqManageTrailingStop(order);            
			sqManageExitAfterXBars(order);               
		}
		
		sqManageOrderExpiration(order);
	}
}

//------------------------------------------------------------------

private Instrument getInstrument(String symbol) {
  if(symbol==null || symbol.equals("NULL") || symbol.equals("Current") || symbol.equals("Same as main chart")) {
      return selectedInstrument;
  }
    /*
  if(!instrumentsMap.containsKey(symbol)) {
	  instrument = Instrument.fromString(symbol);
	  if(instrument==null) throw new Exception("Invalid instrument: ", symbol);
	  
	  instrumentsMap.put(symbol, instrument));
  }
    
  return instrumentsMap.get(symbol);
  */
  
  return selectedInstrument;
}

//------------------------------------------------------------------

private Period getPeriod(int timeframe){
  if(timeframe == 0) {
      return selectedPeriod;
  }
  
  if(!periodsMap.containsKey(timeframe)) {
	 Period _period; 

	 switch(timeframe) {
		case 0:  		_period = Period.TICK; break;
		case 1:  		_period = Period.ONE_MIN; break; 
		case 5:  		_period = Period.FIVE_MINS ; break; 
		case 15:  		_period = Period.FIFTEEN_MINS; break; 
		case 30:  		_period = Period.THIRTY_MINS; break; 
		case 60:  		_period = Period.ONE_HOUR; break; 
		case 240:  		_period = Period.FOUR_HOURS; break; 
		case 1440:  	_period = Period.DAILY; break; 
		case 10080:  	_period = Period.WEEKLY; break;
		case 43200:  	_period = Period.MONTHLY; break;
		default: 		_period = Period.createCustomPeriod(Minute, timeframe); break;
	  }	  
	  
	  periodsMap.put(timeframe, _period);
  }
    
  return periodsMap.get(timeframe);
}

//------------------------------------------------------------------

private Period getSymbolPeriod(String symbol){
  //todo  
  return selectedPeriod; 
}

//------------------------------------------------------------------

private int sqGetDirectionFromOrderType(OrderCommand orderType) {
   if(orderType.isLong()) {
      return 1;
   } else {
      return -1;
   }
}

//------------------------------------------------------------------

private String getLabel(Instrument instr, int magicNumber) {
    String label = getStrategyName() + "_" + instr.name() + "_" + (counter++) + "_" + magicNumber;
    label = label.toUpperCase();
    return label;
}

//------------------------------------------------------------------    
private String getStrategyName() {
    return this.getClass().getSimpleName();
}


//------------------------------------------------------------------------

private double round(double amount, int decimals) {
    return (new BigDecimal(amount)).setScale(decimals, BigDecimal.ROUND_HALF_UP).doubleValue();
}

//+------------------------------------------------------------------+

double roundDown(double value, double step, int decimals) {
	value = Math.floor(value / step) * step;
	
	return roundDown(value, decimals);
}

//------------------------------------------------------------------

private double roundDown(double value, int decimals) {
	//return (new BigDecimal(amount)).setScale(decimals, BigDecimal.ROUND_HALF_DOWN).doubleValue();
	
	double p = 0;
  	
  	switch(decimals) {
  		case 0: return (int) value; 
  		case 1: p = 10; break;
  		case 2: p = 100; break;
  		case 3: p = 1000; break;
  		case 4: p = 10000; break;
  		case 5: p = 100000; break;
  		case 6: p = 1000000; break;
  		default: p = Math.pow(10, decimals);
  	}

  	value = value * p;
  	double tmp = Math.floor(value + 0.00000001);
  	return round(tmp/p, decimals);
}

//------------------------------------------------------------------------

private double roundToPippette(double amount, Instrument instrument) {
    return round(amount, instrument.getPipScale() + 1);
}

//------------------------------------------------------------------

private double sqConvertToRealPips(Instrument instrument, double value) {
   return value * instrument.getPipValue();
}

//------------------------------------------------------------------

private double sqConvertToRealPips(String symbol, double value) {
   instrument = getInstrument(symbol);
   
   return sqConvertToRealPips(instrument, value);
}

//------------------------------------------------------------------+

private int sqConvertToPips(Instrument instrument, double value) {
   return (int)(value / instrument.getPipValue());
}  

//------------------------------------------------------------------+

private int sqConvertToPips(String symbol, double value) {
   instrument = getInstrument(symbol);

   return sqConvertToPips(instrument, value);
}  

//------------------------------------------------------------------

private double sqFixMarketPrice(double price, Instrument instr) {
	BigDecimal bd = new BigDecimal(price);
	bd = bd.setScale(instr.getPipScale() + 1, RoundingMode.HALF_UP);
	return bd.doubleValue();
}

//+------------------------------------------------------------------+

double fixLotSize(String symbol, double size){
   return size;
}

//------------------------------------------------------------------

private boolean sqIsUptrend(String symbol, int timeframe, int method) throws JFException {
   if(method == 0) {
      return (sqClose(symbol, timeframe, 1) > sqMA(symbol, timeframe, 200, 0, MODE_SMA, CLOSE, 1));      
   }

   return false;	
}

//------------------------------------------------------------------

private boolean sqIsDowntrend(String symbol, int timeframe, int method) throws JFException {
   if(method == 0) {
      return (sqClose(symbol, timeframe, 1) < sqMA(symbol, timeframe, 200, 0, MODE_SMA, CLOSE, 1));      
   }

   return false;	
}

//------------------------------------------------------------------

private long monthFirstTradingDay = -1;

private boolean isMonthFirstTradingDay(String symbol, int timeframe, boolean includeWeekends) throws JFException {
	long date = SQTime.getDateInMs(getCurrentTime());
	
	if(monthFirstTradingDay == -1) {
		monthFirstTradingDay = date;
	}

	if(SQTime.getMonth(monthFirstTradingDay)!=SQTime.getMonth(date)) {
		if(!includeWeekends) {
			if(SQTime.getDayOfWeek(date) != SQTime.SATURDAY && SQTime.getDayOfWeek(date) != SQTime.SUNDAY) {
				monthFirstTradingDay = date;
			}
		} else {
			monthFirstTradingDay = date;
		}
	}

	return monthFirstTradingDay==date;
}

//------------------------------------------------------------------

private double roundPips(double pips) {
	BigDecimal bd = new BigDecimal(pips);
	bd = bd.setScale(1, RoundingMode.HALF_UP);
	return bd.doubleValue();
}

//------------------------------------------------------------------

private void sqSetSLandPT(IOrder order, double sl, double pt) throws JFException {
   if(sl == 0 && pt == 0) return;
   
   if(sl == order.getOpenPrice()) {
      print("SL is the same as order price, cannot set it, so we'll delete the order!");
	  order.close();

      return;
   }

   if(pt == order.getOpenPrice()) {
      pt = 0;
   }
   
   if(sl > 0 || pt > 0) {
      boolean result = sqOrderModify(order, order.getOpenPrice(), sqFixMarketPrice(sl, order.getInstrument()), sqFixMarketPrice(pt, order.getInstrument()));
      if(!result) {
         print("Cannot set SL / PT for this order, deleting it!");
         order.close();
      }
   }
}

//------------------------------------------------------------------

private double sqGetSLPercentLevel(String symbol, OrderCommand orderType, double price, int valueInPips, double value) throws JFException {
   String correctedSymbol = correctSymbol(symbol);
   if(price == 0) {
      // price can be zero for market order
      if(orderType.isLong()) {
         price = sqGetAsk(correctedSymbol);
      } else {
         price = sqGetBid(correctedSymbol);
      }
   }
   double gap = price * value / 100;
   return sqGetSLPTLevel(-1.0, symbol, orderType, price, 2, gap);
}

//------------------------------------------------------------------

private double sqGetPTPercentLevel(String symbol, OrderCommand orderType, double price, int valueInPips, double value) throws JFException {
   String correctedSymbol = correctSymbol(symbol);
   if(price == 0) {
      // price can be zero for market order
      if(orderType.isLong()) {
         price = sqGetAsk(correctedSymbol);
      } else {
         price = sqGetBid(correctedSymbol);
      }
   }
   double gap = price * value / 100;
   return sqGetSLPTLevel(1.0, symbol, orderType, price, 2, gap);
}

//------------------------------------------------------------------

private double sqGetSLLevel(String symbol, OrderCommand orderType, double price, int valueInPips, double value) throws JFException {
   return sqGetSLPTLevel(-1.0, symbol, orderType, price, valueInPips, value);
}

//------------------------------------------------------------------

private double sqGetPTLevel(String symbol, OrderCommand orderType, double price, int valueInPips, double value) throws JFException {
   return sqGetSLPTLevel(1.0, symbol, orderType, price, valueInPips, value);
}

//------------------------------------------------------------------

/**
* valueType: 1 - pips, 2 - real pips (ATR range), 3 - price level
*/
private double sqGetSLPTLevel(double SLorPT, String symbol, OrderCommand orderType, double price, int valueType, double value) throws JFException {
   String correctedSymbol = correctSymbol(symbol);
   instrument = getInstrument(correctedSymbol);

   double pointCoef = instrument.getPipValue();

   if(valueType == 1) {
      // convert from pips to real points
      value = sqConvertToRealPips(instrument, value);
   }
   
   if(price == 0) {
      // price can be zero for market order
      if(orderType.isLong()) {
         price = sqGetAsk(correctedSymbol);
      } else {
         price = sqGetBid(correctedSymbol);
      }
   }
   
   double slptValue = value;
   
   if(valueType != 3) {
      if(orderType.isLong()) {
         slptValue = price + (SLorPT * value);
      } else {
         slptValue = price - (SLorPT * value);
      }
   }

   // check that SL / PT is within predefined boundaries
   double minSLPTValue, maxSLPTValue;
   
   if(SLorPT < 0) {
      // it is SL
      
      if(MinimumSL <= 0) {
         minSLPTValue = slptValue;
      } else {
         if(orderType.isLong()) {
            minSLPTValue = price + (SLorPT * MinimumSL * pointCoef);
            slptValue = Math.min(slptValue, minSLPTValue);
            
         } else {
         
            minSLPTValue = price - (SLorPT * MinimumSL * pointCoef);
            slptValue = Math.max(slptValue, minSLPTValue);
         }
   
      }
      
      if(MaximumSL <= 0) {
         maxSLPTValue = slptValue;
      } else {
         if(orderType.isLong()) {
            maxSLPTValue = price + (SLorPT * MaximumSL * pointCoef);
            slptValue = Math.max(slptValue, maxSLPTValue);

         } else {
            maxSLPTValue = price - (SLorPT * MaximumSL * pointCoef);
            slptValue = Math.min(slptValue, maxSLPTValue);
         }

      }
      
   } else {
      // it is PT

      if(MinimumPT <= 0) {
         minSLPTValue = slptValue;
      } else {
         if(orderType.isLong()) {
            minSLPTValue = price + (SLorPT * MinimumPT * pointCoef);
            slptValue = Math.max(slptValue, minSLPTValue);
            
         } else {
            minSLPTValue = price - (SLorPT * MinimumPT * pointCoef);
            slptValue = Math.min(slptValue, minSLPTValue);
         }

      }
      
      if(MaximumPT <= 0) {
         maxSLPTValue = slptValue;
      } else {

         if(orderType.isLong()) {
            maxSLPTValue = price + (SLorPT * MaximumPT * pointCoef);
            slptValue = Math.min(slptValue, maxSLPTValue);

         } else {
         
            maxSLPTValue = price - (SLorPT * MaximumPT * pointCoef);
            slptValue = Math.max(slptValue, maxSLPTValue);
         }
      }
   }
               
   return slptValue;
}  

//------------------------------------------------------------------

private void sqSetGlobalVariable(String ticket, String name, double value) {
   globalVariables.put(ticket+"_"+name, value);
}

//------------------------------------------------------------------

private double sqGetGlobalVariable(String ticket, String name) {
   Double value = globalVariables.get(ticket+"_"+name);
   if(value==null) value = 0d;
   
   return value;
}

//------------------------------------------------------------------

private int sqGetGlobalIntVariable(String ticket, String name) {
   Double value = globalVariables.get(ticket+"_"+name);
   if(value==null) value = 0d;
   
   return value.intValue();
}

//------------------------------------------------------------------

private long sqGetGlobalLongVariable(String ticket, String name) {
   Double value = globalVariables.get(ticket+"_"+name);
   if(value==null) value = 0d;
   
   return value.longValue();
}


//------------------------------------------------------------------

private boolean sqDoublesAreEqual(double value1, double value2){
	return Math.abs(value1 - value2) < 0.00000001;
}

//------------------------------------------------------------------+

private int sqStringHash(String str){
   int h = 0, k;
   for (int i=0; i<str.length(); i++){
      k = Character.getNumericValue(str.charAt(i));
      h = (h << 5) + h + k;
   }
   return h;
}

//------------------------------------------------------------------

private int sqGetOrderExpiration(String ticket) {
   return sqGetGlobalIntVariable(ticket, "sqOrderExpiration");
}

//------------------------------------------------------------------

private void sqSetOrderExpiration(String ticket, int bars) {
   sqSetGlobalVariable(ticket, "sqOrderExpiration", bars);
}

//------------------------------------------------------------------

private void sqCloseAllPositions(String symbol, int magicNumber, int direction, String comment) throws JFException {
   int count = 100; // maximum number of positions to close
   String lastTicket = "";

   while(count > 0) {
      count--;
	  
	  order = sqGetOrder(magicNumber, symbol, direction, comment, false, false);	  
      if(order==null) {
         // no position found
         break;
      }

      if(lastTicket.equals(order.getLabel())) {
         // trying to close the same position one more time, there must be some error
         break;
      }
      lastTicket = order.getLabel();

	  orderType = order.getOrderCommand();
      if(!orderType.isConditional()) {
         sqClosePositionAtMarket(order, 0, "Close all positions");
      } else {
         sqDeletePendingOrder(order, "Close all positions");
      }
   }
}

//------------------------------------------------------------------

private void sqCloseBestPosition(String symbol, int magicNumber, int direction, String comment) throws JFException {
   IOrder bestOrder = null;
   
   orders = getOrders();	

   for (int cc = orders.size() - 1; cc >= 0; cc--) {
	 order = orders.get(cc);
	 orderType = order.getOrderCommand();

	 // skip pending orders
	 if(orderType.isConditional()) {
		continue;
	 }

	 if(magicNumber != 0) {
		if(!checkOrderMagicNumber(order, magicNumber)) continue;
	 }  
	 else if(!checkMagicNumber(getOrderMagicNumber(order))) continue;
	 
	 if(bestOrder==null || order.getProfitLossInAccountCurrency() > bestOrder.getProfitLossInAccountCurrency()) {
		// found order with better profit
		bestOrder = order;
		print("Better position found, ticket: ", order.getLabel(),", PL: ", order.getProfitLossInAccountCurrency());
	 }
   }

   if(bestOrder!=null) {
      sqClosePositionAtMarket(bestOrder, 0, "Close best position");
   }
}

//------------------------------------------------------------------

private void sqCloseWorstPosition(String symbol, int magicNumber, int direction, String comment) throws JFException {
   IOrder worstOrder = null;

   orders = getOrders();	

   for (int cc = orders.size() - 1; cc >= 0; cc--) {
	 order = orders.get(cc);
	 orderType = order.getOrderCommand();

	 // skip pending orders
	 if(orderType.isConditional()) {
		continue;
	 }

	 if(magicNumber != 0) {
		if(!checkOrderMagicNumber(order, magicNumber)) continue;
	 }  
	 else if(!checkMagicNumber(getOrderMagicNumber(order))) continue;
         
	 if(worstOrder==null || order.getProfitLossInAccountCurrency() < worstOrder.getProfitLossInAccountCurrency()) {
		// found order with worse profit
		worstOrder = order;
		print("Worse position found, ticket: ", order.getLabel(),", PL: ", order.getProfitLossInAccountCurrency());
	 }
   }

   if(worstOrder!=null) {
      sqClosePositionAtMarket(worstOrder, 0, "Close worst position");
   }
}

//------------------------------------------------------------------

private int sqGetMarketPosition(String symbol, int magicNumber, String comment) throws JFException {
   order = sqGetOrder(magicNumber, symbol, 0, comment, false);
   if(order!=null) {
      if(order.getOrderCommand().isLong()) {
         return 1;
      } else {
         return -1;
      }
   }
   
   return 0;
}                     

//------------------------------------------------------------------

boolean sqMarketPositionIsShort(int magicNo, String symbol, String comment) throws JFException {
   return sqGetOrder(magicNo, symbol, -1, comment, false)!=null;
}    

//------------------------------------------------------------------

private boolean sqMarketPositionIsNotShort(int magicNo, String symbol, String comment) throws JFException {
	if(sqGetOrder(magicNo, symbol, -1, comment, false)!=null) {
      return false; 	
	}
	 
	 return true;
}     

//------------------------------------------------------------------

private boolean sqMarketPositionIsLong(int magicNo, String symbol, String comment) throws JFException {
   return sqGetOrder(magicNo, symbol, 1, comment, false)!=null;
}     

//------------------------------------------------------------------

private boolean sqMarketPositionIsNotLong(int magicNo, String symbol, String comment) throws JFException {
   if(sqGetOrder(magicNo, symbol, 1, comment, false)!=null) {
      return false; 	
	 }
	 
	 return true;
}     

//------------------------------------------------------------------

private boolean sqMarketPositionIsFlat(int magicNo, String symbol, String comment) throws JFException {
   return sqGetMarketPosition(symbol, magicNo, comment) == 0;
}

//------------------------------------------------------------------

private double sqGetOrderOpenPrice(String symbol, int magicNumber, int direction, String comment) throws JFException {
   order = sqGetOrder(magicNumber, symbol, 0, comment, false);
   if(order!=null) {
      return order.getOpenPrice();
   }
   return -1;
}

//------------------------------------------------------------------

private double sqGetOrderStopLoss(String symbol, int magicNumber, int direction, String comment) throws JFException {
   order = sqGetOrder(magicNumber, symbol, 0, comment, false);
   if(order!=null) {
      return order.getStopLossPrice();
   }
   return -1;
}

//------------------------------------------------------------------

private double sqGetOrderProfitTarget(String symbol, int magicNumber, int direction, String comment) throws JFException {
   order = sqGetOrder(magicNumber, symbol, 0, comment, false);
   if(order!=null) {
      return order.getTakeProfitPrice();
   }
   return -1;
}

//+------------------------------------------------------------------+

double sqGetMarketPositionSize(String symbol, int magicNumber, int direction, String comment) {
   double lots = 0;

   for (int cc = orders.size() - 1; cc >= 0; cc--) {
	 order = orders.get(cc);
	 orderType = order.getOrderCommand();

	 // skip pending orders
	 if(orderType.isConditional()) {
		continue;
	 }
	 
	 if(direction != 0) {
		if(direction > 0 && !orderType.isLong()) continue;
		if(direction < 0 && !orderType.isShort()) continue;
	 }

	 if(magicNumber != 0) {
		if(!checkOrderMagicNumber(order, magicNumber)) continue;
	 }  
	 else if(!checkMagicNumber(getOrderMagicNumber(order))) continue;

	 if(!symbol.equals("Any")) {
		if(order.getInstrument() != getInstrument(symbol)) continue;
	 }

	 if(!comment.isEmpty()) {
		if(order.getComment().indexOf(comment) < 0) continue;
	 }

	 lots += order.getAmount() * 10;
   }

   return lots;
}

//------------------------------------------------------------------

private double sqGetOpenPL(String symbol, int magicNumber, int direction, String comment) throws JFException {
   double pl = 0;

   orders = getOrders();	

   for (int cc = orders.size() - 1; cc >= 0; cc--) {
		 order = orders.get(cc);
		 orderType = order.getOrderCommand();
		 
         // skip pending orders
         if(orderType.isConditional()) {
            continue;
         }

         if(direction != 0) {
            if(direction > 0 && !orderType.isLong()) continue;
            if(direction < 0 && !orderType.isShort()) continue;
         }

         if(magicNumber != 0) {
            if(!checkOrderMagicNumber(order, magicNumber)) continue;
         }  
         else if(!checkMagicNumber(getOrderMagicNumber(order))) continue;

         if(!symbol.equals("Any")) {
            if(order.getInstrument() != getInstrument(symbol)) continue;
         }

         if(!comment.isEmpty()) {
            if(order.getComment().indexOf(comment) < 0) continue;
         }

         pl += order.getProfitLossInAccountCurrency();
   }

   return pl;
}

//------------------------------------------------------------------

private double sqGetOpenPLInPips(String symbol, int magicNumber, int direction, String comment) throws JFException {
   double pl = 0;

   orders = getOrders();	

   for (int cc = orders.size() - 1; cc >= 0; cc--) {
		 order = orders.get(cc);
		 orderType = order.getOrderCommand();

         // skip pending orders
         if(orderType.isConditional()) {
            continue;
         }

         if(direction != 0) {
            if(direction > 0 && !orderType.isLong()) continue;
            if(direction < 0 && !orderType.isShort()) continue;
         }

         if(magicNumber != 0) {
            if(!checkOrderMagicNumber(order, magicNumber)) continue;
         }  
         else if(!checkMagicNumber(getOrderMagicNumber(order))) continue;

         if(!symbol.equals("Any")) {
            if(order.getInstrument() != getInstrument(symbol)) continue;
         }

         if(!comment.isEmpty()) {
            if(order.getComment().indexOf(comment) < 0) continue;
         }

         if(orderType.isLong()) {
            pl += sqGetBid(order.getInstrument()) - order.getOpenPrice();
         } else {
            pl += order.getOpenPrice() - sqGetAsk(order.getInstrument());
         }
   }

   return sqConvertToPips(order.getInstrument(), pl);
}

//------------------------------------------------------------------

private int sqGetClosedPLInPips(String symbol, int magicNumber, int direction, String comment, int shift) throws JFException {
   int index = 0;

   orders = sqGetHistoryOrders(symbol);

   for (int cc = orders.size() - 1; cc >= 0; cc--) {
		 order = orders.get(cc);
		 orderType = order.getOrderCommand();

         // skip pending orders
         if(orderType.isConditional()) {
            continue;
         }

         if(direction != 0) {
            if(direction > 0 && !orderType.isLong()) continue;
            if(direction < 0 && !orderType.isShort()) continue;
         }

         if(magicNumber != 0) {
            if(!checkOrderMagicNumber(order, magicNumber)) continue;
         }  
         else if(!checkMagicNumber(getOrderMagicNumber(order))) continue;

         if(!symbol.equals("Any")) {
            if(order.getInstrument() != getInstrument(symbol)) continue;
         }

         if(!comment.isEmpty()) {
            if(order.getComment().indexOf(comment) < 0) continue;
         }

         if(index == shift) {
            if(orderType.isLong()) {
               return sqConvertToPips(order.getInstrument(), order.getClosePrice() - order.getOpenPrice());
            } else {
               return sqConvertToPips(order.getInstrument(), order.getOpenPrice() - order.getClosePrice());
            }
         }

         index++;
   }

   return 0;
}

//------------------------------------------------------------------
private List<IOrder> sqGetHistoryOrders(String symbol) throws JFException {
	List<IOrder> historyOrders = new ArrayList<IOrder>();
	
	long from = 0;
	long to = System.currentTimeMillis();

	if(symbol.equals("Any")) {
		for (Instrument instrument : context.getSubscribedInstruments()){
			historyOrders.addAll(history.getOrdersHistory(instrument, from, to));
		}
	} else {
		instrument = getInstrument(symbol);
		historyOrders.addAll(history.getOrdersHistory(instrument, from, to));
	}
    
    //filter orders by strategy
    IOrder o;
    for (int cc = historyOrders.size() - 1; cc >= 0; cc--) {
		 o = historyOrders.get(cc);
         
         if(!o.getLabel().toUpperCase().startsWith(getStrategyName().toUpperCase())) {
            historyOrders.remove(cc);
        }
    }
	
	return historyOrders;
}

//------------------------------------------------------------------

private double sqGetClosedPLInMoney(String symbol, int magicNumber, int direction, String comment, int shift) throws JFException {
   int index = 0;

   orders = sqGetHistoryOrders(symbol);

   for (int cc = orders.size() - 1; cc >= 0; cc--) {
		 order = orders.get(cc);
		 orderType = order.getOrderCommand();

         // skip pending orders
         if(orderType.isConditional()) {
            continue;
         }

         if(direction != 0) {
            if(direction > 0 && !orderType.isLong()) continue;
            if(direction < 0 && !orderType.isShort()) continue;
         }

         if(magicNumber != 0) {
            if(!checkOrderMagicNumber(order, magicNumber)) continue;
         }  
         else if(!checkMagicNumber(getOrderMagicNumber(order))) continue;

         if(!symbol.equals("Any")) {
            if(order.getInstrument() != getInstrument(symbol)) continue;
         }

         if(!comment.isEmpty()) {
            if(order.getComment().indexOf(comment) < 0) continue;
         }

         if(index == shift) {
            return order.getProfitLossInAccountCurrency();
         }

         index++;
   }

   return 0;
}

//------------------------------------------------------------------

private int sqGetMarketPositionCount(String symbol, int magicNumber, int direction, String comment) throws JFException {
   int count = 0;

   orders = getOrders();	

   for (int cc = orders.size() - 1; cc >= 0; cc--) {
		 order = orders.get(cc);
		 orderType = order.getOrderCommand();

         // skip pending orders
         if(orderType.isConditional()) {
            continue;
         }

         if(direction != 0) {
            if(direction > 0 && !orderType.isLong()) continue;
            if(direction < 0 && !orderType.isShort()) continue;
         }

         if(magicNumber != 0) {
            if(!checkOrderMagicNumber(order, magicNumber)) continue;
         }  
         else if(!checkMagicNumber(getOrderMagicNumber(order))) continue;

         if(!symbol.equals("Any")) {
            if(order.getInstrument() != getInstrument(symbol)) continue;
         }

         if(!comment.isEmpty()) {
            if(order.getComment().indexOf(comment) < 0) continue;
         }

         count++;
   }

   return count;
}

//------------------------------------------------------------------

private int sqGetBarsSinceOpen(String symbol, int magicNumber, int direction, String comment) throws JFException {
   order = sqGetOrderInHistory(magicNumber, symbol, direction, comment);	
	
   if(order!=null) {
		long opTime = order.getFillTime();

		int numberOfBars = 0;
		int limit = 1000;

		List<IBar> bars = history.getBars(selectedInstrument, selectedPeriod, OfferSide.BID, Filter.ALL_FLATS, limit, history.getStartTimeOfCurrentBar(selectedInstrument, selectedPeriod), 0);

		for(int i=0; i<bars.size(); i++) {
			if(opTime < bars.get(i).getTime()) {
				numberOfBars++;
			}
		}

		return numberOfBars;
   }

   return -1;
}

//------------------------------------------------------------------

private int sqGetBarsSinceClose(String symbol, int magicNumber, int direction, String comment) throws JFException {
   order = sqGetOrderInHistory(magicNumber, symbol, direction, comment);	
	
   if(order!=null) {
		long clTime = order.getCloseTime();

		int numberOfBars = 0;
		int limit = 1000;

		List<IBar> bars = history.getBars(selectedInstrument, selectedPeriod, OfferSide.BID, Filter.ALL_FLATS, limit, history.getStartTimeOfCurrentBar(selectedInstrument, selectedPeriod), 0);

		for(int i=0; i<bars.size(); i++) {
			if(clTime < bars.get(i).getTime()) {
				numberOfBars++;
			}
		}

		return numberOfBars;
   }

   return -1;
}

//------------------------------------------------------------------

private int sqGetLastOrderType(String symbol, int magicNumber, String comment) throws JFException {
   order = sqGetOrderInHistory(magicNumber, symbol, 0, comment);
	
   if(order!=null) {
	  orderType = order.getOrderCommand(); 
	   
	  if(orderType.isLong()) {
         return 1;
      } else {
         return -1;
      }
   }

   return 0;
}

//------------------------------------------------------------------

private IOrder sqGetOrderInHistory(int magicNumber, String symbol, int direction, String comment) throws JFException {
   orders = sqGetHistoryOrders(symbol);

   for (int cc = orders.size() - 1; cc >= 0; cc--) {
		 order = orders.get(cc);
		 orderType = order.getOrderCommand();

		 // skip pending orders
		 if(orderType.isConditional()) {
			continue;
		 }

		 if(direction != 0) {
            if(direction > 0 && !orderType.isLong()) continue;
            if(direction < 0 && !orderType.isShort()) continue;
		 }

		 if(magicNumber != 0) {
			if(!checkOrderMagicNumber(order, magicNumber)) continue;
		 }  
		 else if(!checkMagicNumber(getOrderMagicNumber(order))) continue;

		 if(!symbol.equals("Any")) {
			if(order.getInstrument() != getInstrument(symbol)) continue;
		 }

		 if(!comment.isEmpty()) {
			if(order.getComment().indexOf(comment) < 0) continue;
		 }

		// otherwise we found the order
		return order;
   }

   return null;
}

//------------------------------------------------------------------

private boolean sqTradeRecentlyClosed(String symbol, int magicNumber, boolean checkThisBar, boolean checkThisMinute) throws JFException {
   int ordersChecked = 0;

   long currentTimeMinutes = SQTime.getTimeMinutes(getCurrentTime());

   orders = sqGetHistoryOrders(symbol);

   for (int cc = orders.size() - 1; cc >= 0; cc--) {
		order = orders.get(cc);
		orderType = order.getOrderCommand();

		// skip pending orders
		if(orderType.isConditional()) {
			continue;
		}

		if(magicNumber != 0) {
			if(!checkOrderMagicNumber(order, magicNumber)) continue;
		}  
		else if(!checkMagicNumber(getOrderMagicNumber(order))) continue;

		if(!symbol.equals("Any")) {
			if(order.getInstrument() != getInstrument(symbol)) continue;
		}

		ordersChecked++;
		if(ordersChecked > 10) {
			// check only the very last 10 orders
			break;
		}

		if(checkThisBar) {
			if(order.getCloseTime() >= getBarTime(symbol, 0, 0)) {
				// order finished this bar
				return true;
			}
		}

		if(checkThisMinute) {
		   long orderTimeMinutes = SQTime.getTimeMinutes(order.getCloseTime());

			if(currentTimeMinutes == orderTimeMinutes) {
				// order finished this minute
				return true;
			}
		}
   }

   return false;
}

//------------------------------------------------------------------

private double sqHighest(String symbol, int timeframe, AppliedPrice computedFrom, int period, int shift) throws JFException {
   double maxnum = -100000000;
   double val;

   for(int i=shift; i<shift+period; i++) {
      val = sqGetValue(symbol, timeframe, computedFrom, i);

      if(val > maxnum) {
         maxnum = val;
      }
   }

   return maxnum;
}

//------------------------------------------------------------------

private int sqHighestIndex(String symbol, int timeframe, AppliedPrice computedFrom, int period, int shift) throws JFException {
   double maxnum = -100000000;
   int index = 0;
   double val;

   for(int i=shift; i<shift+period; i++) {
      val = sqGetValue(symbol, timeframe, computedFrom, i);

      if(val > maxnum) {
         maxnum = val;
         index = i;
      }
   }

   return index;
}

//------------------------------------------------------------------

private double sqLowest(String symbol, int timeframe, AppliedPrice computedFrom, int period, int shift) throws JFException {
   double minnum = 100000000;
   double val;

   for(int i=shift; i<shift+period; i++) {
      val = sqGetValue(symbol, timeframe, computedFrom, i);

      if(val < minnum) {
         minnum = val;
      }
   }

   return minnum;
}

//------------------------------------------------------------------

private int sqLowestIndex(String symbol, int timeframe, AppliedPrice computedFrom, int period, int shift) throws JFException {
   double minnum = 100000000;
   int index = 0;
   double val;

   for(int i=shift; i<shift+period; i++) {
      val = sqGetValue(symbol, timeframe, computedFrom, i);

      if(val < minnum) {
         minnum = val;
         index = i;
      }
   }

   return index;
}

//-----------------------------------------------------------------

private double sqGetValue(String symbol, int timeframe, AppliedPrice computedFrom, int shift) throws JFException {
   double val = 0;
   
   switch(computedFrom) {
	 case OPEN: val = sqOpen(symbol, timeframe, shift); break;
	 case HIGH: val = sqHigh(symbol, timeframe, shift); break;
	 case LOW: val = sqLow(symbol, timeframe, shift); break;
	 case CLOSE: val = sqClose(symbol, timeframe, shift); break;
	 case MEDIAN_PRICE: val = (sqHigh(symbol, timeframe, shift)+sqLow(symbol, timeframe, shift))/2; break;
	 case TYPICAL_PRICE: val = (sqHigh(symbol, timeframe, shift)+sqLow(symbol, timeframe, shift)+sqClose(symbol, timeframe, shift))/3; break;
	 case WEIGHTED_CLOSE: val = (sqHigh(symbol, timeframe, shift)+sqLow(symbol, timeframe, shift)+sqClose(symbol, timeframe, shift)+sqClose(symbol, timeframe, shift))/4; break;
   }
   
   return roundValue(val);
}

//+------------------------------------------------------------------+

double sqDaily(String symbol, int tf, AppliedPrice computedFrom, int shift) throws JFException {
   return sqGetValue(symbol, 1440, computedFrom, shift);
}                

//+------------------------------------------------------------------+

double sqWeekly(String symbol, int tf, AppliedPrice computedFrom, int shift) throws JFException {
   return sqGetValue(symbol, 10080, computedFrom, shift);
}

//+------------------------------------------------------------------+

double sqMonthly(String symbol, int tf, AppliedPrice computedFrom, int shift) throws JFException {
   return sqGetValue(symbol, 43200, computedFrom, shift);
}

//------------------------------------------------------------------

private double getPointValue(Instrument instrument) throws JFException {
	double price = context.getUtils().convertPipToCurrency(instrument, context.getAccount().getCurrency());
	
	return price / instrument.getPipValue() * getLotSize(instrument);
}

//------------------------------------------------------------------

private double getLotSize(Instrument instrument) throws JFException {
    if(instrument.getGroup().isCrypto()) return 1d; 
    
    if(instrument.getGroup().isForex()) return 100000d;
    
    return 1d;
}

//------------------------------------------------------------------

private double sizeToAmount(double size, Instrument instrument) throws JFException {
    return size * getLotSize(instrument) / 1000000d;
}

//------------------------------------------------------------------

private long getBarTime(String symbol, int timeframe, int shift) throws JFException {
	return getBar(symbol, timeframe, shift).getTime();
}

//------------------------------------------------------------------

private long getCurrentTime() throws JFException {
	return history.getStartTimeOfCurrentBar(selectedInstrument, selectedPeriod);
}

//------------------------------------------------------------------

private void sqManageOrderExpiration(IOrder order) throws JFException {
   int tempValue = 0;                                                           
   int barsOpen = 0;

   ticket = order.getLabel();
   orderType = order.getOrderCommand();
	
   // Stop/Limit Order Expiration
   if(orderType.isConditional()) {
      // handle only pending orders
      tempValue = sqGetOrderExpiration(ticket);
      if(tempValue > 0) {
         barsOpen = sqGetOpenBarsForOrder(order, tempValue+10);
		 
         if(barsOpen >= tempValue) {
            print("Order with ticket: ", ticket, " expired");
            sqDeletePendingOrder(order, "Expired, bars valid: " + tempValue);
         }
      }
   }
}

//------------------------------------------------------------------

private int sqGetOpenBarsForOrder(IOrder order, int expBarsPeriod) throws JFException {
   long opTime;

   if(!orderType.isConditional()) { //live order
       opTime = order.getFillTime();
   } else { //pending order
       opTime = sqGetGlobalLongVariable(ticket, "sqOrderOpenTime");
   }

   int numberOfBars = 0;
   int limit = expBarsPeriod + 10;
      
   List<IBar> bars = history.getBars(selectedInstrument, selectedPeriod, OfferSide.BID, Filter.ALL_FLATS, limit, history.getStartTimeOfCurrentBar(selectedInstrument, selectedPeriod), 0);

   for(int i=0; i<bars.size(); i++) {
      if(opTime < bars.get(i).getTime()) {
         numberOfBars++;
      }
   }

   return numberOfBars;
}
	
//------------------------------------------------------------------

private boolean sqEvaluateFuzzySignal(int conditionsCount, int minTrueConditions) {

	boolean signalValue = false;
  int trueConditionsCount = 0;
   
  if(minTrueConditions <= 0) {
  	minTrueConditions = 1;
  }
	    
	for(int i=0; i<conditionsCount; i++) {
		boolean value = cond[i];
				
		if(value) {
			trueConditionsCount++;
		}
				
		if(trueConditionsCount >= minTrueConditions) {
			signalValue = true;
			break;
		}
	}
			
	return signalValue;
}

//------------------------------------------------------------------

double sqSafeDivide(double var1, double var2) {
   if(var2 == 0) return(100000000);
   return(var1/var2);
}

//------------------------------------------------------------------

private boolean sqIsBarOpen() {
	return true;
}
	
//------------------------------------------------------------------
// -- SQTime
//------------------------------------------------------------------
static private class SQTime {
	public static final int MONDAY = 1;
	public static final int TUESDAY = 2;
	public static final int WEDNESDAY = 3;
	public static final int THURSDAY = 4;
	public static final int FRIDAY = 5;
	public static final int SATURDAY = 6;
	public static final int SUNDAY = 0;
	
	/** The Constant formaterDateMinute. */
	private static final SimpleDateFormat formaterDateMinute = new SimpleDateFormat("yyyy.MM.dd HH:mm");

	private static final SimpleDateFormat formaterFullDateTime = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS");
	
	private static final SimpleDateFormat formaterFullDateMinute = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
	
	/** The Constant formaterDate. */
	private static final SimpleDateFormat formaterDate = new SimpleDateFormat("yyyy.MM.dd");

	private static Calendar cal;
    
	static {
        cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
    }

	//------------------------------------------------------------------
	
	public static String toDateMinuteString(long datetime) {
		return formaterDateMinute.format(datetime);
	}

	//------------------------------------------------------------------

	public static String toFullDateMinuteString(long datetime) {
		return formaterFullDateMinute.format(datetime);
	}
	
	//------------------------------------------------------------------
	
	public static String toFullDateTimeString(long datetime) {
		return formaterFullDateTime.format(datetime);
	}

	//------------------------------------------------------------------

	public static String toDateString(long datetime) {
		return formaterDate.format(datetime);
	}
	
	//------------------------------------------------------------------
	
	public static int getDate(long datetime) {
		return (int) (datetime / 1000);
	}
	
	//------------------------------------------------------------------

	public static int getYear(long datetime) {
		cal.setTimeInMillis(datetime);
		return cal.get(Calendar.YEAR);
	}
	
	//------------------------------------------------------------------
	
	public static int getMonth(long datetime) {
		cal.setTimeInMillis(datetime);
		return cal.get(Calendar.MONTH) + 1;
	}
	
	//------------------------------------------------------------------
	
	public static int getDayOfWeek(long datetime) {
		cal.setTimeInMillis(datetime);
		int dow = cal.get(Calendar.DAY_OF_WEEK);

		return dow - 1;
	}
	
	//------------------------------------------------------------------
	
	public static int getDay(long datetime) {
		cal.setTimeInMillis(datetime);
		return cal.get(Calendar.DAY_OF_YEAR);
	}
	
	//------------------------------------------------------------------
	
	public static int getHour(long datetime) {
		cal.setTimeInMillis(datetime);
		return cal.get(Calendar.HOUR_OF_DAY);
	}
	
	//------------------------------------------------------------------
	
	public static int getMinute(long datetime) {
		cal.setTimeInMillis(datetime);
		return cal.get(Calendar.MINUTE);
	}
	
	//------------------------------------------------------------------
	
	public static int getSecond(long datetime) {
		cal.setTimeInMillis(datetime);
		return cal.get(Calendar.SECOND);
	}
	
	//------------------------------------------------------------------
	
	public static int getTime(long datetime) {
		return (int) (datetime % (24*60*60*1000L));
	}
	
	//------------------------------------------------------------------
	
	public static int getTime(int hour, int minute, int second) {
		return hour*60*60*1000 + minute*60*1000 + second*1000;
	}

	//------------------------------------------------------------------
	
	public static long getTimeMinutes(long datetime) {
		cal.setTimeInMillis(datetime);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTimeInMillis();
	}

	// ------------------------------------------------------------------------

	public static long correctDayEnd(long datetime) {
		return setTime(datetime, 23, 59, 59, 999);
	}

	// ------------------------------------------------------------------------

	public static long correctDayStart(long datetime) {
		return setTime(datetime, 0, 0, 0, 0);
	}

	// ------------------------------------------------------------------------

	public static long setHHMM(long datetime, String hhmm) {
		String[] parts = hhmm.split(":");
		int hours = Integer.parseInt(parts[0]);
		int minutes = Integer.parseInt(parts[1]);

		cal.setTimeInMillis(datetime);
		cal.set(Calendar.HOUR_OF_DAY, hours);
		cal.set(Calendar.MINUTE, minutes);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		
		return cal.getTimeInMillis();
	}
	
	// ------------------------------------------------------------------------
	
	public static long setTime(long datetime, int hour, int minute, int second) {
		cal.setTimeInMillis(datetime);
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, minute);
		cal.set(Calendar.SECOND, second);
		cal.set(Calendar.MILLISECOND, 0);
		
		return cal.getTimeInMillis();
	}

	// ------------------------------------------------------------------------

	public static long setTime(long datetime, int hour, int minute, int second, int miliseconds) {
		cal.setTimeInMillis(datetime);
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, minute);
		cal.set(Calendar.SECOND, second);
		cal.set(Calendar.MILLISECOND, miliseconds);
				
		return cal.getTimeInMillis();
	}
	
	// ------------------------------------------------------------------------

	public static long setDayOfWeek(long datetime, int dow) {
		dow = dow + 1;
		
		cal.setTimeInMillis(datetime);
		cal.set(Calendar.DAY_OF_WEEK, dow);
		
		return cal.getTimeInMillis();
	}
	
	// ------------------------------------------------------------------------
	
	public static long addDays(long datetime, int days) {
		cal.setTimeInMillis(datetime);
		cal.add(Calendar.DATE, days);
		
		return cal.getTimeInMillis();
	}
	
	// ------------------------------------------------------------------------

	public static long getDateInMs(long datetime) {
		return setTime(datetime, 0, 0, 0, 0);
	}

}

//------------------------------------------------------------------

private int getHHMM(String time){
   String[] result = time.split(":");          
   
   if(result.length == 2){
      int hour = Integer.parseInt(result[0].trim());
      int minute = Integer.parseInt(result[1].trim());
      return (hour * 100) + minute;
	  
   } else {
      print("Incorrect time value format. Value: '" + time + "'");
      return 0;
   }
}

//------------------------------------------------------------------

private int sqGetDate(int day, int month, int year) {
   return (year - 1900) * 10000 + month * 100 + day;
}

//------------------------------------------------------------------

private double sqGetTime(int hour, int minute, int second) {
   return 100 * hour + minute + 0.01 * second;
}

//------------------------------------------------------------------

private int getSQTime(long time){
   int minutesToday = (int) (time / 60) % (24 * 60);
   int hours = minutesToday / 60;
   int minutes = minutesToday % 60;
   
   return hours*100 + minutes;
}

//------------------------------------------------------------------
// -- ITradingOption
//------------------------------------------------------------------
public interface ITradingOption {
	public boolean onTick() throws JFException;
}
