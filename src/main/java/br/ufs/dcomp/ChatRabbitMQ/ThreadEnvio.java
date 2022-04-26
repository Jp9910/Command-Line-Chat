package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Channel;

public class ThreadEnvio extends Thread
{
    private String nomeDaThread;
    private Channel canal;
    private String usuario;
    
    public ThreadEnvio(String nome, Channel canal, String usuario){
        this.nomeDaThread = nome;
        this.usuario = usuario;
        this.canal = canal;
    }
    public void run(){
        try{
            int x = 1;
        } catch (Exception e){
            System.out.println(e);
        }
    }
}