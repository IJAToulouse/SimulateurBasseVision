package ija.lbled.simulateurbassevision;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * Created by l.bled on 28/05/2015.
 */
@Root(name="root")
public class Configuration {
    @Element(name="acuite")
    private double acuite;
    @Element(name="scotome")
    private int scotome;
    @Element(name="scotomeSB")
    private int scotomeSB;
    @Element(name="isScotome")
    private boolean isScotome;
    @Element(name="visionTubu")
    private int visionTubu;
    @Element(name="visionTubuSB")
    private int visionTubuSB;
    @Element(name="isVisionTubu")
    private boolean isVisionTubu;
    @Element(name="hemianopsie")
    private int hemianopsie;
    @Element(name="hemianopsieSB")
    private int hemianopsieSB;
    @Element(name="isHemianopsie")
    private boolean isHemianopsie;
    @Element(name="hemianopsieID")
    private int hemianopsieID;
    @Element(name="contraste")
    private double contraste;
    @Element(name="contrasteSB")
    private int contrasteSB;
    @Element(name="luminosite")
    private int luminosite;
    @Element(name="luminositeSB")
    private int luminositeSB;
    @Element(name="isNiveauDeGris")
    private boolean isNiveauDeGris;

    public double getAcuite() {
        return acuite;
    }

    public void setAcuite(double acuite) {
        this.acuite = acuite;
    }

    public int getScotome() {
        return scotome;
    }

    public void setScotome(int scotome) {
        this.scotome = scotome;
    }

    public int getScotomeSB() {
        return scotomeSB;
    }

    public void setScotomeSB(int scotomeSB) {
        this.scotomeSB = scotomeSB;
    }

    public boolean isScotome() {
        return isScotome;
    }

    public void setIsScotome(boolean isScotome) {
        this.isScotome = isScotome;
        if (isScotome) {
            setIsVisionTubu(false);
            setIsHemianopsie(false);
        }
    }

    public int getVisionTubu() {
        return visionTubu;
    }

    public void setVisionTubu(int visionTubu) {
        this.visionTubu = visionTubu;
    }

    public int getVisionTubuSB() {
        return visionTubuSB;
    }

    public void setVisionTubuSB(int visionTubuSB) {
        this.visionTubuSB = visionTubuSB;
    }

    public boolean isVisionTubu() {
        return isVisionTubu;
    }

    public void setIsVisionTubu(boolean isVisionTubu) {
        this.isVisionTubu = isVisionTubu;
        if (isVisionTubu) {
            setIsHemianopsie(false);
            setIsScotome(false);
        }
    }

    public int getHemianopsie() {
        return hemianopsie;
    }

    public void setHemianopsie(int hemianopsie) {
        this.hemianopsie = hemianopsie;
    }

    public int getHemianopsieSB() {
        return hemianopsieSB;
    }

    public void setHemianopsieSB(int hemianopsieSB) {
        this.hemianopsieSB = hemianopsieSB;
    }

    public boolean isHemianopsie() {
        return isHemianopsie;
    }

    public void setIsHemianopsie(boolean isHemianopsie) {
        this.isHemianopsie = isHemianopsie;
        if (isHemianopsie) {
            setIsVisionTubu(false);
            setIsScotome(false);
        }
    }

    public int getHemianopsieID() {
        return hemianopsieID;
    }

    public void setHemianopsieID(int hemianopsieID) {
        this.hemianopsieID = hemianopsieID;
    }

    public double getContraste() {
        return contraste;
    }

    public void setContraste(double contraste) {
        this.contraste = contraste;
    }

    public int getContrasteSB() {
        return contrasteSB;
    }

    public void setContrasteSB(int contrasteSB) {
        this.contrasteSB = contrasteSB;
    }

    public int getLuminosite() {
        return luminosite;
    }

    public void setLuminosite(int luminosite) {
        this.luminosite = luminosite;
    }

    public int getLuminositeSB() {
        return luminositeSB;
    }

    public void setLuminositeSB(int luminositeSB) {
        this.luminositeSB = luminositeSB;
    }

    public boolean isNiveauDeGris() {
        return isNiveauDeGris;
    }

    public void setIsNiveauDeGris(boolean isNiveauDeGris) {
        this.isNiveauDeGris = isNiveauDeGris;
    }
}
