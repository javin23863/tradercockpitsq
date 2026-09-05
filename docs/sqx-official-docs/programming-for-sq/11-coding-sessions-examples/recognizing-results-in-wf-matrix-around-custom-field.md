# Recognizing results in WF Matrix around custom field

- Source: <https://strategyquant.com/doc/programming-for-sq/recognizing-results-in-wf-matrix-around-custom-field/>
- Section: Programming for StrategyQuant X › Coding sessions examples
- Fetched: 2026-09-04

---

The original request:

*is there a way to choose a specific cell in WFM which a certain number of surronding cells have to have Pass?*  
*i.e. I want to have a passed WFM if 4 cells that surrond the 10 Runs and 20%OOS cell have a pass result. I don’t want that SQX choose the best cell according to surrounding results, but instead I want to define which cell I need to have surrounding passed cells.*

Yes, it is possible, the easiest way to do it is by using Custom Analysis snippet that can be started right after the optimization.

Commented CustomAnalysis snippet code:

```
package SQ.CustomAnalysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.tradinglib.*;
import com.strategyquant.tradinglib.optimization.WalkForwardMatrixResult;

public class CAWFMCheck extends CustomAnalysisMethod {
    public static final Logger Log = LoggerFactory.getLogger("CAWFMCheck");
    
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    
    public CAWFMCheck() {
        super("CA WFM check", TYPE_FILTER_STRATEGY);
    }
    
    //------------------------------------------------------------------------
    
    @Override
    public boolean filterStrategy(String project, String task, String databankName, ResultsGroup rg) throws Exception {

        // how many neighbours must pass for WF Matrix to pass?
        int neighboursToPass = 3;

        // find the wanted WF result in strategy ResultsGroup - in our case 10 Runs and 20% OOS
        WalkForwardResult wfResult;
        try {
            wfResult = findWFResult(rg, 20, 10); // 10 Runs and 20%OOS
        } catch(Exception e) {
            Log.info("WFResult not found: " + e.getMessage());

            return true;
        }

        // create WFM matrix as two dimensional array
        WalkForwardMatrixResult wfm = (WalkForwardMatrixResult) rg.mainResult().get(SettingsKeys.WalkForwardResult);
        WalkForwardResult[][] wfMatrix = createMatrix(wfm);
        
        int matrixRows = wfMatrix.length;
        int matrixColumns = wfMatrix[0].length;

        // now go through this two-dimensional array and look for neighbours of our chosen WF result
        for(int rowIndex=0; rowIndex<matrixRows; rowIndex++) {						
            for(int colIndex=0; colIndex<matrixColumns; colIndex++) {
                
                if(wfMatrix[rowIndex][colIndex].param1 == wfResult.param1 && wfMatrix[rowIndex][colIndex].param2 == wfResult.param2) {
                    //WF result position found in the matrix, check neighbours
                    int neighboursPassed = 0;
                    int neighboursChecked = 0;
                    
                    for(int neighbourRowIndex=rowIndex-1; neighbourRowIndex<=rowIndex+1; neighbourRowIndex++) {
                        if(neighbourRowIndex<0 || neighbourRowIndex>=matrixRows) {
                            continue;
                        }
                        
                        for(int neighbourColIndex=colIndex-1; neighbourColIndex<=colIndex+1; neighbourColIndex++) {
                            if(neighbourColIndex<0 || neighbourColIndex>=matrixColumns) {
                                continue;
                            }
                            
                            if(neighbourRowIndex==rowIndex && neighbourColIndex==colIndex) {
                                continue;
                            }
                            
                            WalkForwardResult wfResultNeighbour = wfMatrix[neighbourRowIndex][neighbourColIndex];
                            if(wfResultNeighbour.passed) {
                                neighboursPassed++;
                            }
                            
                            neighboursChecked++;
                        }
                    }
                    
                    Log.info(String.format("Neighbours checked %d / passed %d", neighboursChecked, neighboursPassed));

                    // found the WF Result, passed neighbours checked. Compare to threshold value
                    return neighboursPassed >= neighboursToPass;
                }
            }
        }
        
        return false;
    }
    
    //------------------------------------------------------------------------
    
    private WalkForwardResult findWFResult(ResultsGroup rg, int param1, int param2) throws Exception {
        WalkForwardMatrixResult wfm = (WalkForwardMatrixResult) rg.mainResult().get(SettingsKeys.WalkForwardResult);
        if(wfm==null) {
            throw new Exception("WFMatrixResult not found.");
        }
        
        return wfm.getWFResult(param1, param2);
    }
    
    //------------------------------------------------------------------------

    private WalkForwardResult[][] createMatrix(WalkForwardMatrixResult wfm) throws Exception {
        int rows = 0;
        int columns = 0;

        for(int j=wfm.start2; j<=wfm.stop2; j+=wfm.increment2) rows++;
        for(int i=wfm.start1; i<=wfm.stop1; i+=wfm.increment1) columns++;
            
        WalkForwardResult[][] wfMatrix = new WalkForwardResult[rows][columns];

        int param1Index = 0;
        int param2Index = 0;
        
        for(int j=wfm.start2; j<=wfm.stop2; j+=wfm.increment2) {		
            for(int i=wfm.start1; i<=wfm.stop1; i+=wfm.increment1) {
                WalkForwardResult wfResult = wfm.getWFResult(i, j);
              				
                wfMatrix[param2Index][param1Index] = wfResult; 
                param1Index++;				
            }	
                        
            param2Index++;
            param1Index=0;
        }
        
        return wfMatrix;
    }
}
```
