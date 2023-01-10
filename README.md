Configuration:<br />
enter the symbol,strikePrice,expiryDate,pointDifference,strikePriceVariation in the InputData.csv file placed at /src/test/resources/InputData.csv <br />
Sample data : <br />
script,symbol,strikePrice,expiryDate,pointDifference,strikePriceVariation <br />
ExtractDerivativeData,NIFTY,18100,12-Jan-2023,50,7 <br />
ExtractDerivativeData,BANKNIFTY,42600,12-Jan-2023,100,7 <br />
<br />
Open Terminal <br />
Navigate to the downloaded nseOptionData-develop folder by entering cd /path to folder <br />
Enter below command to run <br />
for i in {1..120}; do echo -n "This iteration $i : "; date;gradle cleanTest test; sleep 180; done <br />

Results: <br />
Excle files will be created separately for the NIFTY and BANKNIFTY for each day in the logs folder <br />