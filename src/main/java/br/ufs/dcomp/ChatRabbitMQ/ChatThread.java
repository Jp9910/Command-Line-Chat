package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Channel;

public class ChatThread extends Thread
{
    private String nomeDaThread;
    private Channel canal;
    private Consumer consumer;
    private String usuario;
    
    public ChatThread(String nome, Channel canal, Consumer consumer, String usuario){
        this.nomeDaThread = nome;
        this.consumer = consumer;
        this.usuario = usuario;
        this.canal = canal;
    }
    public void run(){
        try{
                                //(queue-name, autoAck, consumer);    
            canal.basicConsume(usuario+"-T", true,    consumer); //consumir da fila recebida (de texto ou de arquivo)
        } catch (Exception e){
            System.out.println(e);
        }
    }
}