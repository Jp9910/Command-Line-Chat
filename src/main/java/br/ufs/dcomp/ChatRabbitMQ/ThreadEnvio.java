package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Channel;

public class ThreadEnvio extends Thread
{
    private String nomeDaThread;
    private Channel canal;
    private String usuario;
    private String pessoaDest;
    private String grupoDest;
    private String tipoDeMensagem; // "-F" = mensagem de arquivo, "-T" = mensagem de texto
    private String caminho;
    
    public ThreadEnvio(String nome, Channel canal, String usuario, String pessoaDest, String grupoDest, String tipoDeMensagem, String caminho){
        this.nomeDaThread = nome;
        this.usuario = usuario;
        this.canal = canal;
        this.pessoaDest = pessoaDest;
        this.grupoDest = grupoDest;
        this.tipoDeMensagem = tipoDeMensagem;
        this.caminho = caminho;
    }
    public void run(){
        try {
                boolean isGroupMessage = pessoaDest.isEmpty();
                MensagemProto.Mensagem mensagem_construida;
                if(isGroupMessage)
                    mensagem_construida = Chat.MontarMensagemArquivo(caminho,usuario,grupoDest);
                else
                    mensagem_construida = Chat.MontarMensagemArquivo(caminho,usuario, null);
                
                //Serializar a Mensagem em um vetor de bytes antes de enviar
                byte[] mensagem_serializada = mensagem_construida.toByteArray();
                
                if(isGroupMessage){
                    canal.basicPublish(grupoDest,tipoDeMensagem,null,mensagem_serializada); //mensagem para grupo
                    System.out.println("Arquivo "+caminho+" foi enviado para o grupo #"+grupoDest);
                }
                else {
                    canal.basicPublish("",pessoaDest+tipoDeMensagem,null,mensagem_serializada); //mensagem para pessoa
                    System.out.println("Arquivo "+caminho+" foi enviado para o usuário @"+pessoaDest);
                }
                System.out.print("[thread]"+Chat.getPrompt());
        } 
        catch (Exception e) {
                System.out.println(" !! Erro na thread de envio. !! ");
                //e.printStackTrace();
                System.out.print("[thread]"+Chat.getPrompt());
        }
    }
}