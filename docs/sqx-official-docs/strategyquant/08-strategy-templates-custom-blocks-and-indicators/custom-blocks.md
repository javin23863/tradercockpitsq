# Custom blocks

- Source: <https://strategyquant.com/doc/strategyquant/custom-blocks/>
- Section: StrategyQuant X › Strategy templates, custom blocks and indicators
- Fetched: 2026-09-04

---

**What are Custom blocks?**

Custom blocks allow you to create your own combination of blocks – and store this combination as your own custom block.

For example, look at already existing build-in signal block in StrategyQuant:

**CCI is rising**

This signal is valid when:

```
CCI(Period)[2] < CCI(Period][1]
```

where Period is configurable and [2] means two bars back, [1] means one bar back.

So, it is valid when CCI one bar back is higher than CCI two bars back – which means value of CCI is rising.

**What is the advantage of creating own custom block combinations?**

The value lies in decreasing randomness by using signals that make some real sense.

StrategyQuant could generate condition like this randomly using just blocks **CCI** and **IsLower (<)**, but the chance of generating MEANINGFUL condition randomly is small when you consider how many random possibilities there are.

Another advantage is that when you use block ***CCI(Period) is rising***, from the outside it has only one Period parameter that will be used in both CCI indicators in the condition.

You can extend StrategyQuant by creating your own custom blocks like this by making a snippet in Java code. This is a good way, but not everybody is a programmer.  
So, since SQ Build 127 there is a way of creating Custom blocks like this in a visual way in AlgoWizard editor without any coding.

In this documentation we’ll show how exactly to do it.

## Step 1: Define your new Custom block

In StrategyQuant open the embedded **AlgoWizard** (1.), click on **Customize** (2.) menu on the top. Then switch to **Custom blocks** (3.) tab:

[![](https://strategyquant.com/wp-content/uploads/2020/04/cb_editor-1024x552.png)](https://strategyquant.com/wp-content/uploads/2020/04/cb_editor-1024x552.png)

This is Custom blocks management and edit panel.

As an example, we’ll create a new custom block that will be valid when CCI is rising for 3 consecutive bars. In pseudo code we can write this condition as:

```
CCI(Period)[3] < CCI(Period) [2] < CCI(Period) [1]
```

Click on the **Add block** button. This will open a dialog, where we can specify parameters of our new block:  
[![](https://strategyquant.com/wp-content/uploads/2020/04/c321_add-300x196.png)](https://strategyquant.com/wp-content/uploads/2020/04/c321_add-300x196.png)

#### **Block key**

is unique key (ID) of this block. It can consist only of letters and numbers, o space or special characters. We’ll name our new block CCIRising3Bars.

#### **Block name**

is its name that we’ll see in Building blocks selection. We’ll name it CCI rising for 3 bars

#### **Block type**

is a type block. There are two supported types for now:

- **Condition** – block that returns true/false value that can be used as a part of entry or exit conditions. It is usually some comparison. Block of this type will be visible in Signals section of Building blocks.
- **Price level** – block that computes price level that can be used for stop / limit orders. Block of this type is visible in Stop / Limit Price Levels section in Stop / Limit Entry blocks category in Building blocks configuration.

Click **Save** and new custom block will be created:

[![SQ Custom block added](https://strategyquant.com/wp-content/uploads/2020/04/c321_add2-1024x142.png)](https://strategyquant.com/wp-content/uploads/2020/04/c321_add2-1024x142.png)

Note that this block is yet empty, we created a definition but the new block has no content yet.

Let’s add first condition to the block by clicking on **Add another condition**:

[![Custom block add condition](https://strategyquant.com/wp-content/uploads/2020/04/c321_add3.png)](https://strategyquant.com/wp-content/uploads/2020/04/c321_add3.png)

This opens a standard dialog for choosing from all the existing building blocks that you know from AlgoWizard editor. Let’s find **CCI** indicator for our first comparison:

[![](https://strategyquant.com/wp-content/uploads/2020/04/c321_add4.png)](https://strategyquant.com/wp-content/uploads/2020/04/c321_add4.png)

Don’t forget to set Shift to 3, because we want to get CCI value from 3 bars back. After clicking on **Confirm** the first part of our custom condition is created:

[![](https://strategyquant.com/wp-content/uploads/2020/04/c321_add5-1024x146.png)](https://strategyquant.com/wp-content/uploads/2020/04/c321_add5-1024x146.png)

Now click on **Is Greater (>)** comparison block and change it to **Is Lower (<)**.

Then click on **0** block and replace it with **CCI(14)[2]**.

Repeat these two steps to add a second condition **CCI(14)[2] < CCI(14)[1]**.

The result should look like this:

[![](https://strategyquant.com/wp-content/uploads/2020/04/c321_add6-300x105.png)](https://strategyquant.com/wp-content/uploads/2020/04/c321_add6-300x105.png)

This is a complete condition. It is valid when CCI(14) is rising for the last 3 bars.

You might have noticed that CCI period is set to a fixed value 14.  
If we were to use this custom block at its current state it would look like this:

[![](https://strategyquant.com/wp-content/uploads/2020/04/c321_noparams-300x215.png)](https://strategyquant.com/wp-content/uploads/2020/04/c321_noparams-300x215.png)

Our new custom block would have only mandatory Chart parameter, and no other parameters.

This means that CCI period used in the block will be always 14 – the period will be **not generated randomly** in StrategyQuant and you cannot configure it later.  
You might what that for some parameters, but we want CCI period to be randomly generated.

This is where custom block parameters come to play. There is **Parameters** section on the bottom left part:

[![Custom block parameters](https://strategyquant.com/wp-content/uploads/2020/04/c321_params1.png)](https://strategyquant.com/wp-content/uploads/2020/04/c321_params1.png)  
Right now it contains only one parameter – **Chart** – which is mandatory for every custom block.  
Let’s add a new parameter **Period**. Click on **Add parameter** button. This will add a new parameter to the table. Rename it to Period and change its type to period:

[![](https://strategyquant.com/wp-content/uploads/2020/04/c321_params2.png)](https://strategyquant.com/wp-content/uploads/2020/04/c321_params2.png)

You can name your parameter any way you want, and you have a choice of options for a type:

- **chart** – means it is a chart parameter. Indicators are computed from chart data, and chart parameter allows us and StrategyQuant to set up which chart to use.
- **period** – a special type that should be used for indicator periods. It is a separate type because StrategyQuant needs to know that this parameter is a period, so that it will be limited by period range boundaries set in the generator.
- **shift** – another special type that should be used for shifts. Shift is an index to the bars history. Shift=0 means current bar, Shift=1 means previous bar, Sift=2 means two bars back. Again it is a separate type because StrategyQuant needs to know that this parameter is a shift, so that it will be limited by period shift boundaries set in the generator.
- **int, double, bool** – standard available variable types

We added new period parameter but it is not yet used in our custom block conditions, they still have fixed value 14:

[![](https://strategyquant.com/wp-content/uploads/2020/04/c321_params3.png)](https://strategyquant.com/wp-content/uploads/2020/04/c321_params3.png)

To use the parameter in the condition we must double click on every condition to open it for edit. Then click on the blue **[…]** button next to Period parameter. It will open a list where you can choose from possible Block parameters for period. We will choose our new Period parameter.

[![](https://strategyquant.com/wp-content/uploads/2020/04/c321_params4-300x160.png)](https://strategyquant.com/wp-content/uploads/2020/04/c321_params4-300x160.png)

The result is that period in our CCI indicator will now no longer be a fixed value, but it will be externally configurable using Period parameter:

[![](https://strategyquant.com/wp-content/uploads/2020/04/c321_params5-300x170.png)](https://strategyquant.com/wp-content/uploads/2020/04/c321_params5-300x170.png)

Repeat it for the rest of the CCI conditions, the result should look like this:

[![](https://strategyquant.com/wp-content/uploads/2020/04/c321_params6-300x170.png)](https://strategyquant.com/wp-content/uploads/2020/04/c321_params6-300x170.png)  
This way we created a new custom block called **CCI rising for 3 bars**. After you save your custom blocks you can immediately use it in the AlgoWizard editor:

[![](https://strategyquant.com/wp-content/uploads/2020/04/c321_editor-300x148.png)](https://strategyquant.com/wp-content/uploads/2020/04/c321_editor-300x148.png)

As well as in Building blocks configuration of a builder:

[![](https://strategyquant.com/wp-content/uploads/2020/04/c321_bb.png)](https://strategyquant.com/wp-content/uploads/2020/04/c321_bb.png)

The good thing is that you don’t need to do anything else. Because this custom block was created using already existing block components, StrategyQuant knows how to generate source code for it for your target trading platform.

## Step 2: Specifying an opposite block

We created a new custom block **CCI rising for 3 bars**. Trading strategy usually has Long and Short rules, where Short entry conditions are opposite of Long entry conditions.

Every standard block in StrategyQuant has its opposite block specified, so that SQ “knows” how to create an opposite block for a generated one.

If custom block has no opposite block defined, then it is just copied and used also as an opposite block.  
So, your strategy conditions would look like:

```
Long Entry = CCI rising for 3 bars
Short Entry = CCI rising for 3 bars
```

We usually don’t want this, we would want the rules to be like:

```
Long Entry = CCI rising for 3 bars
Short Entry = CCI falling for 3 bars
```

Block **CCI falling for 3 bars** doesn’t exist yet. To make it work we must create it. Fortunately, it is very simple. Go back to custom blocks editor and click on **Not set link**:

[![](https://strategyquant.com/wp-content/uploads/2020/04/c321_opposite-1024x130.png)](https://strategyquant.com/wp-content/uploads/2020/04/c321_opposite-1024x130.png)

This will open a dialog where we can create a new custom block that will be used as an opposite block , or choose an existing one – if it exists.  
We’ll create a new block and name it **CCI falling for 3 bars**:

[![](https://strategyquant.com/wp-content/uploads/2020/04/c321_opposite2.png)](https://strategyquant.com/wp-content/uploads/2020/04/c321_opposite2.png)  
Click on **Save** and the new block is created:

[![](https://strategyquant.com/wp-content/uploads/2020/04/c321_opposite3-1024x131.png)](https://strategyquant.com/wp-content/uploads/2020/04/c321_opposite3-1024x131.png)

Note that it is not empty – StrategyQuant created it by negating the original block for your convenience. The negation is correct so there is nothing left to do now. We have two blocks defined the way we wanted:

### Updating opposite blocks

This automatic negation happens only once when you create the opposite block. If you now made some changes to the block **CCI rising for 3 bars**, these changes are not reflected to its opposite block **CCI falling for 3 bars** automatically.

To apply the changes automatically by again negating the source custom block you can go to the opposite block and click on the **Negate from opposite block** button.

Just note  – you must do this on the opposite block.  
So if you edit block **CCI rising for 3 bars** you should then switch to a block **CCI falling for 3 bars** and there click on the **Negate from opposite block** button to automatically negate the contents of the changed block and use it in the opposite block.
