// Original Seven Bridges Problem at Euler's time
CREATE
    (left:Place{name:"left"}),
    (right:Place{name:"right"}),
    (up:Place{name:"up"}),
    (down:Place{name:"down"}),

    (left)-[:Bridge]->(up),
    (left)<-[:Bridge]-(up),
    (left)-[:Bridge]->(down),
    (left)<-[:Bridge]-(down),
    (left)-[:Bridge]->(right),
    (right)-[:Bridge]->(up),
    (right)-[:Bridge]->(down);
