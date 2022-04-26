package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Channel;

public class ThreadRecebimento extends Thread
{
    private String nomeDaThread;
    private Channel canal;
    private Consumer consumer;
    private String usuario;
    private String tipoDeMensagem;
    
    public ThreadRecebimento(String nome, Channel canal, Consumer consumer, String usuario, String tipoDeMensagem){
        this.nomeDaThread = nome;
        this.consumer = consumer;
        this.usuario = usuario;
        this.canal = canal;
        this.tipoDeMensagem = tipoDeMensagem;
    }
    public void run(){
        try{
                                //(queue-name, autoAck, consumer);    
            canal.basicConsume(usuario+tipoDeMensagem, true,    consumer); //consumir da fila recebida (de texto ou de arquivo)
        } catch (Exception e){
            System.out.println(e);
        }
    }
}