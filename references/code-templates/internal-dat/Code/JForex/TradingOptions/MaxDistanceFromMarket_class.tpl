

//------------------------------------------------------------------
// -- CMaxDistanceFromMarket
//------------------------------------------------------------------
private class CMaxDistanceFromMarket implements ITradingOption {

	public CMaxDistanceFromMarket() {

	}

	//------------------------------------------------------------------------

	@Override
	public boolean onTick() throws JFException {
		return true;
	}      
}

// create variable for class instance (required)
private CMaxDistanceFromMarket objMaxDistanceFromMarket = new CMaxDistanceFromMarket();