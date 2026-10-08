-- V10: vinculo entre a ficha da parceira e o ponto do Google Places escolhido no mapa.
-- O place_id pode ser guardado indefinidamente pelos Termos do Google (diferente de nome, endereco
-- e coordenada), por isso e persistido aqui. Indice unico parcial: uma mesma ficha de Places nao
-- pode estar ligada a duas parceiras, mas parceiras sem ponto (NULL) continuam validas.
ALTER TABLE recycling_business ADD COLUMN place_id VARCHAR(200);

CREATE UNIQUE INDEX ux_recycling_business_place_id ON recycling_business (place_id) WHERE place_id IS NOT NULL;

