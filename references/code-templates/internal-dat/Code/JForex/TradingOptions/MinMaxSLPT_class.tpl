

//------------------------------------------------------------------
// -- CMinMaxSLPT
//------------------------------------------------------------------
private class  CMinMaxSLPT implements ITradingOption {

	private CMinMaxSLPT() {
	}

	//------------------------------------------------------------------------

	@Override
	public boolean onTick() throws JFException {
		return true;
	}
}

// create variable for class instance (required)
private CMinMaxSLPT objMinMaxSLPT = new CMinMaxSLPT();