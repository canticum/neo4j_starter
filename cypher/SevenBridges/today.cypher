// Remove two bridges from Euler's version for today
MATCH (:Place{name:"left"})<-[r]-() 
DELETE r;
