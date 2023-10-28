package poo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.awt.event.KeyListener;

import java.net.URL;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.Timer;

/**
 * Trata-se de um exemplo simples e não representa uma boa prática de
 * programação, pois a classe está com muita responsabilidade. O ideal
 * seria separar a lógica do jogo da parte gráfica e criar uma classe
 * para cada componente, elemento do jogo.
 * 
 * Tem-se ainda alguns problemas de desempenho, pois o arquivo de áudio é
 * carregado a cada colisão com o bloco de ferro. O ideal seria carregar
 * o arquivo de áudio uma única vez. O mesmo acontece com a fonte.
 * O ideal seria carregar a fonte uma única vez.
 */
public class App extends JPanel implements KeyListener {

    /**
     * Número de quadros por segundo (frames per second)
     */
    private static final int FPS = 60;

    // bola que se move
    private int bolaCoordenadaX;
    private int bolaCoordenadaY;
    private int velocidadeX;
    private int velocidadeY;
    private final Image imagemBola = carregarImagem("imagens/bola.png");
    private final int larguraBola = imagemBola.getWidth(null);
    private final int alturaBola = imagemBola.getHeight(null);

    // bloco de ferro que se move para a direita ou para a esquerda (usuário
    // controla)
    private final Image blocoFerro = carregarImagem("imagens/ferro.png");
    private final int blocoLargura = blocoFerro.getWidth(null);
    private final int blocoAltura = blocoFerro.getHeight(null);
    private int blocoCoordenadaX;
    private int blocoCoordenadaY;
    private int delocamentoXDoBloco;

    // contador de batidas da bola no bloco de ferro
    private int contadorDeBatidas = 0;
    // tempo que o contador de batidas fica na tela
    private int tempoDoContadorDeBatidaNaTela = 0;

    private boolean pausado;
    private boolean somLigado;
    private Timer timer;

    private final Image tijolo = carregarImagem("imagens/tijolo.png");
    private int larguraTijolo = tijolo.getWidth(null);
    private int alturaTijolo = tijolo.getHeight(null);
    // 0 = vazio, 1 = tijolo, 2 = poderia ser outro tipo de tijolo
    public short[][] tijolos = { // 11x17 (linha,coluna)
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 0 },
            { 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0 },
            { 0, 2, 2, 1, 1, 1, 2, 1, 1, 1, 2, 1, 1, 1, 2, 2, 0 },
            { 0, 2, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 2, 0 },
            { 0, 2, 2, 1, 1, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 2, 0 },
            { 0, 2, 0, 1, 0, 2, 0, 1, 0, 1, 0, 1, 0, 1, 0, 2, 0 },
            { 0, 2, 2, 1, 2, 2, 2, 1, 1, 1, 2, 1, 1, 1, 2, 2, 0 },
            { 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0 },
            { 0, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }
    };

    private final Image explosao = carregarImagem("imagens/explosao.png");
    private int larguraExplosao = explosao.getWidth(null);
    private int alturaExplosao = explosao.getHeight(null);
    private int tempoDeExplosaoNaTela = 0;

    /**
     * Construtor da classe
     * 
     * @param largura largura da área de desenho
     * @param altura  altura da área de desenho
     */
    public App(int largura, int altura) {

        this.setPreferredSize(new Dimension(largura, altura));
        this.setSize(largura, altura);
        this.setBackground(Color.BLACK);
        setFocusable(true);

        // inicializa as coordenadas do objeto que se move no centro da área de desenho
        this.bolaCoordenadaX = largura / 2;
        this.bolaCoordenadaY = altura / 2;

        // inicializa as coordenadas do bloco de ferro
        this.blocoCoordenadaX = largura / 2;
        this.blocoCoordenadaY = altura - 50;

        // inicializa a velocidade do objeto que se move. A velocidade é medida em
        // pixels
        this.velocidadeX = 2;
        this.velocidadeY = 4;

        this.pausado = false;
        this.somLigado = true;

        // adiciona objeto da própria classe como ouvinte de eventos do teclado
        this.addKeyListener(this);

        // invoca o método paint() dessa classe a cada 1000/FPS milissegundos
        this.timer = new Timer(1000 / FPS, e -> {
            repaint();
        });
        // disparando o timer
        this.timer.start();
    }

    /**
     * Atualiza as coordenadas dos objetos e desenha os objetos na tela
     */
    @Override
    public void paint(Graphics g) {
        var g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        if (!this.pausado) {
            // atualiza as coordenadas do objeto que se move
            this.movimentaObjeto();

            // verifica se colidiu com o bloco
            this.colisaoBola();
            
            // desenha os objetos na tela
            this.desenharObjetos(g2d);
            this.desenharContador(g2d);
            this.desenhaExplosao(g2d);
        } else {
            this.pausar(g2d);
        }

        // sincroniza o contexto gráfico
        Toolkit.getDefaultToolkit().sync();
    }

    /**
     * Atualiza as coordenadas do objeto que se move
     * 
     * ricochetear nas bordas da janela respeitando a leia da colisão elástica?
     * https://pt.wikipedia.org/wiki/Colis%C3%A3o_el%C3%A1stica
     */
    public void movimentaObjeto() {
        this.bolaCoordenadaX += this.velocidadeX;
        this.bolaCoordenadaY += this.velocidadeY;

        // Coordenada (0,0) está no canto superior esquerdo da janela
        // Coordenada (largura, altura) está no canto inferior direito da janela

        // ricochetear bola nas bordas da janela
        if (this.bolaCoordenadaX < 0 || this.bolaCoordenadaX + this.larguraBola > this.getWidth()) {
            this.velocidadeX *= -1;
            this.reproduzirAudio("sons/bola.wav");
        }
        // ricochetear bola nas bordas da janela
        if (this.bolaCoordenadaY < 0 || this.bolaCoordenadaY + this.alturaBola > this.getHeight()) {
            this.velocidadeY *= -1;
            this.reproduzirAudio("sons/bola.wav");
        }

        // movimenta o bloco para a direita ou para a esquerda
        this.blocoCoordenadaX += this.delocamentoXDoBloco;
        if (this.blocoCoordenadaX < 0) {
            this.blocoCoordenadaX = 0;
        }
        if (this.blocoCoordenadaX + this.blocoLargura > this.getWidth()) {
            this.blocoCoordenadaX = this.getWidth() - this.blocoLargura;
        }
    }

    /**
     * Verifica se a bola colidiu com algum objeto
     * Essa implementação possui alguns problemas e deve ser melhorada
     */
    public void colisaoBola() {
        Rectangle bola = new Rectangle(this.bolaCoordenadaX, this.bolaCoordenadaY, this.larguraBola, this.alturaBola);

        // https://docs.oracle.com/javase/7/docs/api/java/awt/Rectangle.html#intersects(java.awt.Rectangle)
        // Tem problema de colisão, pois a bola pode atravessar o bloco
        if (bola.intersects(this.blocoCoordenadaX, this.blocoCoordenadaY, this.blocoLargura, this.blocoAltura)) {
            this.velocidadeX *= -1;
            this.velocidadeY *= -1;
            this.contadorDeBatidas++;
            this.tempoDoContadorDeBatidaNaTela = 100;
            this.reproduzirAudio("sons/metal.wav");
        }

        // verifica se a bola colidiu com o tijolo na posição (2,2)
        if (bola.intersects(this.celulaParaPixel(2), this.celulaParaPixel(2), this.larguraTijolo, this.alturaTijolo)) {
            this.tempoDeExplosaoNaTela = 100;
        }
    }

    /**
     * Desenha os objetos na tela
     * 
     * @param g2d objeto gráfico
     */
    public void desenharObjetos(Graphics2D g2d) {
        // desenha os tijolos
        for (int i = 0; i < this.tijolos.length; i++) {
            for (int j = 0; j < this.tijolos[i].length; j++) {
                if (this.tijolos[i][j] == 1) {
                    g2d.drawImage(tijolo, j * this.larguraTijolo, i * this.alturaTijolo, this.larguraTijolo, this.alturaTijolo,
                            null);
                }
            }
        }

        // desenha o bloco de ferro
        g2d.drawImage(blocoFerro, blocoCoordenadaX, blocoCoordenadaY, blocoLargura, blocoAltura, null);

        // desenha o objeto que se move
        g2d.drawImage(imagemBola, bolaCoordenadaX, bolaCoordenadaY, this.larguraBola, this.alturaBola, null);
    }

    /**
     * Desenha a explosão na tela
     * 
     * @param g2d
     */
    private void desenhaExplosao(Graphics2D g2d) {
        if (this.tempoDeExplosaoNaTela > 0) {
            // desenha a explosão no centro da tela na posição (2,2) por tempoDeExpolsaoNaTela quadros
            g2d.drawImage(explosao, this.celulaParaPixel(2), this.celulaParaPixel(2), this.larguraExplosao,
                    this.alturaExplosao, null);
            this.tempoDeExplosaoNaTela--;
        }
    }

    /**
     * Desenha o contador de batidas no bloco de metal
     * 
     * @param g2d
     */
    private void desenharContador(Graphics2D g2d) {
        if (this.tempoDoContadorDeBatidaNaTela > 0) {
            g2d.setColor(Color.WHITE);
            g2d.draw3DRect(this.getWidth() - 130, 10, 90, 50, true);

            g2d.setColor(Color.RED);
            g2d.setFont(carregaFonteDoDisco("SevenSegment.ttf", 40f));
            g2d.drawString("" + this.contadorDeBatidas, this.getWidth() - 100, 50);
            this.tempoDoContadorDeBatidaNaTela--;
        }
    }

    /**
     * Desenha uma mensagem na tela quando a animação está pausada
     * 
     * @param g2d objeto gráfico
     */
    private void pausar(Graphics2D g2d) {
        var mensagem = "Animação pausada";
        // var font = new Font("Courier", Font.BOLD, 18);
        var font = carregaFonteDoDisco("Nosifer.ttf", 36f);
        java.awt.FontMetrics fontMetrics = this.getFontMetrics(font);
        g2d.setFont(font);

        // g2d.setColor(Color.GRAY);
        // g2d.fill3DRect(100, this.getHeight() / 2 - 100, this.getWidth() - 200, 200, true);

        g2d.setColor(Color.WHITE);
        // desenha a mensagem no centro da área de desenho
        g2d.drawString(mensagem, (this.getWidth() - fontMetrics.stringWidth(mensagem)) / 2, this.getHeight() / 2);
    }

    /**
     * Converte uma coordenada em pixels para uma coordenada em células
     * 
     * @param pixel coordenada em pixels
     * @return coordenada em células
     */
    public int celulaParaPixel(int celula) {
        // considerando que o tijolo tem largura igual a altura
        return larguraTijolo * celula;
    }

    /**
     * Converte uma coordenada em células para uma coordenada em pixels
     * 
     * @param celula coordenada em células
     * @return coordenada em pixels
     */
    public int pixelParaCelula(int pixel) {
        // considerando que o tijolo tem largura igual a altura
        return pixel / larguraTijolo;
    }

    /**
     * Processa uma tecla pressionada
     * 
     * @param e evento de tecla pressionada
     */
    public void processaTeclaPressionada(KeyEvent e) {
        // https://docs.oracle.com/en/java/javase/21/language/switch-expressions.html
        switch (e.getKeyCode()) {
            case KeyEvent.VK_SPACE -> this.pausado = !this.pausado;
            case KeyEvent.VK_RIGHT -> this.delocamentoXDoBloco = 2;
            case KeyEvent.VK_LEFT -> this.delocamentoXDoBloco = -2;
            case KeyEvent.VK_Q -> System.exit(0);
            case KeyEvent.VK_S -> {
                this.somLigado = !this.somLigado;
                System.out.println("Som ligado: " + this.somLigado);
            }
        }
    }

    /**
     * Processa uma tecla solta
     * 
     * @param e evento de tecla solta
     */
    public void processaTeclaSolta(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_RIGHT:
                this.delocamentoXDoBloco = 0;
                break;
            case KeyEvent.VK_LEFT:
                this.delocamentoXDoBloco = 0;
                break;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        processaTeclaPressionada(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        processaTeclaSolta(e);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // não faz nada
    }

    /**
     * Carrega uma imagem
     * 
     * @param arquivo nome do arquivo de imagem que deve estar na pasta
     *                /src/main/resources
     * @return imagem carregada
     */
    private Image carregarImagem(String arquivo) {
        var ii = new ImageIcon(arquivo);

        if ((ii == null) || (ii.getImageLoadStatus() != MediaTracker.COMPLETE)) {
            URL url = getClass().getResource("/" + arquivo);
            if (url == null)
                throw new IllegalArgumentException("imagem " + arquivo + " não encontrada");
            ii = new ImageIcon(url);
        }
        return ii.getImage();
    }

    /**
     * Reproduz um arquivo de áudio
     * 
     * @param arquivo nome do arquivo de áudio que deve estar na pasta
     *                /src/main/resources
     */
    private void reproduzirAudio(String arquivo) {
        if (somLigado) {
            try (var audioIn = AudioSystem.getAudioInputStream(getClass().getResource("/" + arquivo))) {
                Clip clip = AudioSystem.getClip();
                clip.open(audioIn);
                clip.start();
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                System.err.println("Erro ao carregar o arquivo de áudio " + arquivo);
            }
        }
    }

    /**
     * Carrega uma fonte do disco
     * 
     * @param nome nome do arquivo de fonte (com extensão .ttf) que deve estar na
     *             pasta /src/main/resources
     * @return fonte carregada
     */
    private Font carregaFonteDoDisco(String nome, float tamanho) {
        InputStream is = getClass().getResourceAsStream("/fonts/" + nome);
        try {
            var font = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(tamanho);
            return font;
        } catch (FontFormatException | IOException e) {
            System.err.println("erro ao ler font do disco: " + e);
        }
        return null;
    }

    public static void main(String[] args) {
        int largura = 800;
        int altura = 600;

        // cria a janela
        JFrame frame = new JFrame();
        frame.setTitle("Animação Java 2D");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(largura, altura);

        // adiciona a área de desenho (JPanel) na janela
        frame.add(new App(largura, altura));
        
        // não permite que o usuário redimensione a janela
        frame.setResizable(false);
        // ajusta o tamanho da janela para que caiba todos os componentes
        frame.pack();
        // centraliza a janela na tela
        frame.setLocationRelativeTo(null);
        // exibe a janela
        frame.setVisible(true);

        System.out.println("..:: Pressione uma das teclas ::..");
        System.out.println("barra de espaço para pausar");
        System.out.println("tecla 's' para ligar/desligar o som");
        System.out.println("tecla 'seta para direita' para mover para a direita (não implementado)");
        System.out.println("tecla 'seta para esquerda' para mover para a esquerda (não implementado)");
    }
}
