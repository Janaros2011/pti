const express = require('express')
const app = express()
const port = 8080

app.use(express.json());

app.post('/new', (req, res, next) => {
    const fs = require('fs');
    if(fs.existsSync("rentals.json")) {
	rentalsFileRawData = fs.readFileSync('rentals.json');
	rentalsJSON = JSON.parse(rentalsFileRawData);
    }
    else{
   	rentalsJSON = {"rentals": []};
    	fs.writeFileSync("rentals.json", JSON.stringify(rentalsJSON));
    }
    rentalsJSON['rentals'].push({"maker": req.body.maker, "model": req.body.model, "days": req.body.days, "units":req.body.units});
    fs.writeFileSync("rentals.json", JSON.stringify(rentalsJSON));
    console.log(rentalsJSON);
    res.end(); 
}) 

app.get("/list", (req, res, next) => {
    const fs = require('fs');
    try {
	 rentalsFileRawData = fs.readFileSync('rentals.json');
   	 rentalsJSON = JSON.parse(rentalsFileRawData);
   	 for(var i in rentalsJSON['rentals']){
       		rental = rentalsJSON['rentals'][i];
		console.log(rental.maker+'\n'+rental.model+'\n'+rental.days+'\n'+rental.units+'\n');
   	 }
   	 res.json(rentalsJSON);
    }
    catch(error) {
	console.error(error);
        res.end();
    }
});

app.listen(port, () => {
  console.log(`PTI HTTP Server listening at http://localhost:${port}`)
})
