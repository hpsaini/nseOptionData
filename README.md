Configuration:
enter the symbol,strikePrice,expiryDate,pointDifference,strikePriceVariation in the InputData.csv file placed at /src/test/resources/InputData.csv
Sample data :
script,symbol,strikePrice,expiryDate,pointDifference,strikePriceVariation
ExtractDerivativeData,NIFTY,18100,12-Jan-2023,50,7
ExtractDerivativeData,BANKNIFTY,42600,12-Jan-2023,100,7

Open Terminal
Navigate to the downloaded nseOptionData-develop folder by entering cd /path to folder
Enter below command to run
for i in {1..120}; do echo -n "This iteration $i : "; date;gradle cleanTest test; sleep 180; done

Results:
Excle files will be created separately for the NIFTY and BANKNIFTY for each day in the logs folder