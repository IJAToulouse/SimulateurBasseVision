package ija.lbled.simulateurbassevision;

import android.widget.RadioGroup;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * Created by l.bled on 28/05/2015.
 */
@Root(name="root")
public class Configuration {
    @Element(name="acuite")
    private double acuite;
    @Element(name="monRadioGroup")
    private RadioGroup monRadioGroup;
    @Element(name="scotome")
    private int scotome;
    @Element(name="scotomeSB")
    private int scotomeSB;
    /*private int tubulaire;
    private boolean isTubulaire;
    private int hemianopsie;
    private boolean isHemianopsie;*/
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

    public RadioGroup getMonRadioGroup() {
        return monRadioGroup;
    }

    public void setMonRadioGroup(RadioGroup monRadioGroup) {
        this.monRadioGroup = monRadioGroup;
    }

    /*public int getTubulaire() {
        return tubulaire;
    }

    public void setTubulaire(int tubulaire) {
        this.tubulaire = tubulaire;
    }

    public boolean isTubulaire() {
        return isTubulaire;
    }

    public void setIsTubulaire(boolean isTubulaire) {
        this.isTubulaire = isTubulaire;
        if (isTubulaire) {
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

    public boolean isHemianopsie() {
        return isHemianopsie;
    }

    public void setIsHemianopsie(boolean isHemianopsie) {
        this.isHemianopsie = isHemianopsie;
        if (isHemianopsie) {
            setIsTubulaire(false);
            setIsScotome(false);
        }
    }*/

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
