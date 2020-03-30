/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.isis.generated;


import static com.isis.generated.TyperatioType.ANGE;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 *
 * @author remis
 */
public class Services {
        
    static World world = new World();
    
    public World readWorldFromXml(String username) throws JAXBException, FileNotFoundException {
        
        InputStream input;
        JAXBContext cont;
        File file = new File(username+"-"+"world.xml");
        
        if(file.exists()){
            input = new FileInputStream(file);
            System.out.println("J'ouvre le fichier : "+file.getAbsolutePath());
            
        } else { 
            input = getClass().getClassLoader().getResourceAsStream("world.xml");
            System.out.println("Bienvenue au nouveau joueur !"); 
        }
        
        cont= JAXBContext.newInstance(World.class);
        Unmarshaller u = cont.createUnmarshaller();
        world = (World) u.unmarshal(input);
        
        return world;        
    }
    
    public void saveWorldToXml(World world, String username) throws JAXBException, FileNotFoundException, IOException {

        JAXBContext cont;
        
        try{
            OutputStream output = new FileOutputStream(username+"-"+"world.xml");
            cont= JAXBContext.newInstance(World.class);
            Marshaller m = cont.createMarshaller();
            m.marshal(world,output);
            System.out.println("J'enregistre la partie : "+output);
            //output.close();
            
        } catch (Exception ex) {
            System.out.println("Erreur : " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
     @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
     public World getWorld(String username) throws JAXBException, FileNotFoundException, IOException {
        World world = this.readWorldFromXml(username);
        for (ProductType prod : world.getProducts().getProduct()) {
            if (!prod.isManagerUnlocked()) {
                if (prod.getTimeleft() != 0) {
                    if (prod.getTimeleft() < (System.currentTimeMillis() - world.getLastupdate())) {
                        world.setScore(world.getScore() + prod.getRevenu());
                    } else {
                        prod.setTimeleft(prod.getTimeleft() - (System.currentTimeMillis() - world.getLastupdate()));
                    }
                }
            } else {
                long time = System.currentTimeMillis() - world.getLastupdate();
                long nb_prod = (time / prod.getVitesse());
                long time_left = (time % prod.getVitesse());
                world.setScore(world.getScore() + prod.getRevenu() * nb_prod);
                prod.setTimeleft(time_left);
            }
        }
        
        world.setLastupdate(System.currentTimeMillis());
        saveWorldToXml(world, username);
        return this.readWorldFromXml(username);
    }
    
    
    // prend en paramètre le pseudo du joueur et le produit 
    // sur lequel une action a eu lieu (lancement manuel de production ou 
    // achat d’une certaine quantité de produit) 
    // renvoie false si l’action n’a pas pu être traitée 
    public Boolean updateProduct(String username, ProductType newproduct) throws JAXBException, IOException { 
        // aller chercher le monde qui correspond au joueur 
        World world = getWorld(username); 
        // trouver dans ce monde, le produit équivalent à celui passé en paramètre 
        ProductType product = findProductById(world, newproduct.getId()); 
        if (product == null) { return false;} 
        // calculer la variation de quantité. Si elle est positive c'est 
        // que le joueur a acheté une certaine quantité de ce produit 
        // sinon c’est qu’il s’agit d’un lancement de production. 
        int qtchange = newproduct.getQuantite() - product.getQuantite(); 
        if (qtchange > 0) { 
            // soustraire de l'argent du joueur le cout de la quantité 
            // achetée et mettre à jour la quantité de product 
            double cout = (product.getCout() * (1 - Math.pow(product.getCroissance(), product.getQuantite()))) / (1 - product.getCroissance());
            world.setMoney(world.getMoney() - cout);
            product.setQuantite(product.getQuantite() + newproduct.getQuantite());
        } else { 
            // initialiser product.timeleft à product.vitesse 
            // pour lancer la production 
            product.setTimeleft(product.getVitesse());
            product.setQuantite(newproduct.getQuantite());
        } 
        // sauvegarder les changements du monde 
        saveWorldToXml(world, username); 
        return true; 
    }
    
     private ProductType findProductById(World world, int id) {
        ProductType product = null;
        for (ProductType p : world.getProducts().getProduct()) {
            if (p.getId() == id) {
                product = p;
            }
        }
        return product;
    }
     
     public Boolean updateManager(String username, PallierType newmanager) throws JAXBException, FileNotFoundException, IOException {
        // aller chercher le monde qui correspond au joueur
        World world = getWorld(username);
        // trouver dans ce monde, le manager équivalent à celui passé
        // en paramètre
        PallierType manager = findManagerByName(world, newmanager.getName());
        //Page 42
        if (manager == null) {
            return false;
        }
        // débloquer ce manager
        // trouver le produit correspondant au manager
        ProductType product = findProductById(world, manager.getIdcible());
        if (product == null) {
            return false;
        }
        // débloquer le manager de ce produit
        product.setManagerUnlocked(true);
        manager.setUnlocked(true);
        // soustraire de l'argent du joueur le cout du manager
        world.setMoney(world.getMoney()-manager.getSeuil());
        // sauvegarder les changements au monde
        saveWorldToXml(world, username);
        return true;
    }
    
     private PallierType findManagerByName(World world, String name) {
        PallierType manager = null;
        for (PallierType palier : world.getManagers().getPallier()) {
            if (palier.getName().equals(name)) {
                manager = palier;
            }
        }
        return manager;
    }
     
    public Boolean updateUpgrade(String username, PallierType upgrade) throws JAXBException, FileNotFoundException, IOException {
        World world = getWorld(username);
        PallierType u = findUpgradeByName(world, upgrade.getName());
        ProductType product = findProductById(world, upgrade.getIdcible());
        TyperatioType type = u.getTyperatio();

        //On vérifie que l'upgrade existe. Si il n'existe pas
        //il n'y a pas besoin d'effectuer tous les tests suivants.
        if(u == null){ return false; }
        //Cet upgrade ne touche que les anges
        //Par sécurité, on vérifie bien que le Typeratio est ANGE
        if (upgrade.getRatio() == -1) {
            if (type == ANGE) {
                world.setAngelbonus((int) (world.getAngelbonus() + upgrade.getRatio()));
            }
        //On applique ici l'upgrade à tous les produits du monde.
        } else if (upgrade.getRatio() == 0) {
            for (ProductType prod : world.getProducts().getProduct()) {
                switch (type) {
                    case VITESSE:
                        prod.setVitesse((int) (prod.getVitesse() * upgrade.getRatio()));
                    case GAIN:
                        prod.setRevenu(prod.getRevenu() * upgrade.getRatio());
                }
            }
        } else {
            //On cherche le produit associé à l'upgrade
            for (ProductType p : world.getProducts().getProduct()) {
                if (p.getId() == upgrade.getIdcible()) {
                    product = p;
                }
            }
            if (product == null) { return false; }
            
            //Si le produit existe, on vient lui appliquer les upgrades.
            switch (type) {
                case VITESSE:
                    product.setVitesse((int) (product.getVitesse() * upgrade.getRatio()));
                case GAIN:
                    product.setRevenu(product.getRevenu() * upgrade.getRatio());
                case ANGE:
                    world.setAngelbonus((int) (world.getAngelbonus() + upgrade.getRatio()));
            }
        }
        upgrade.setUnlocked(true);
        saveWorldToXml(world, username);
        return true;
    }
    
    private PallierType findUpgradeByName(World world, String name) {
        PallierType upgrade = null;
        for (PallierType p : world.getUpgrades().getPallier()) {
            if (p.getName().equals(name)) {
                upgrade = p;
            }
        }
        return upgrade;
    }
    
}
