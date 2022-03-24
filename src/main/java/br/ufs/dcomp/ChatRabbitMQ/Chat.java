package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;
import java.io.IOException;

import java.util.Scanner;
import java.time.LocalDate;
import java.time.LocalTime;
import com.google.protobuf.ByteString;

public class Chat {

  private static String prompt = new String("");
  
  public static void main(String[] argv) throws Exception 
  {
    
    Scanner sc = new Scanner(System.in);
    
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("54.235.231.127");
    factory.setUsername("jp");
    factory.setPassword("9910");
    factory.setVirtualHost("/");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    
    String USUARIO = "";
    while(USUARIO.isEmpty())
    {
      System.out.print("User: ");
      USUARIO = sc.nextLine();
    }
                      //(queue-name, durable, exclusive, auto-delete, params); 
    channel.queueDeclare(USUARIO, false,   false,     false,       null);
    
    Consumer consumer = new DefaultConsumer(channel) 
    {
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException 
      {
        // O que o receptor fará quando chegar uma mensagem. "body" é o conteúdo da mensagem (representado em bytes)
        // Desserializar o conteúdo recebido
        MensagemProto.Mensagem mensagem_recebida = MensagemProto.Mensagem.parseFrom(body);
        String emissor = mensagem_recebida.getEmissor();
        String data = mensagem_recebida.getData();
        String hora = mensagem_recebida.getHora();
        
        MensagemProto.Conteudo conteudo = mensagem_recebida.getConteudo();
        String texto = conteudo.getCorpo().toStringUtf8();
        
        // Formato da mensagem: (21/09/2016 às 20:53) marciocosta diz: E aí, Tarcisio! Vamos sim!
        String msg_formatada = (  "("+data+" às "+hora+") "+emissor+" diz: "+texto   );
        System.out.println("\n"+msg_formatada);
        System.out.print(prompt);
      }
    };
                      //(queue-name, autoAck, consumer);    
    channel.basicConsume(USUARIO, true,    consumer);
    
    String destinatario = "";
    int dest_compare = -1;
    while(destinatario.isEmpty() || dest_compare!=0) //Enquanto o primeiro caractere for vazio ou não for '@'
    {
        System.out.println("Digite '@<destinatário>' para escolher o destinatário. Ex: @joao");
        prompt = ">> ";
        System.out.print(prompt);
        destinatario = sc.nextLine();
        if(!destinatario.isEmpty())
          dest_compare = Character.compare(destinatario.charAt(0),'@');
    }
    destinatario = destinatario.substring(1);
    System.out.println("Enviando mensagem para "+destinatario+". Para trocar o destinatario digite '@<destinatario>'. Para sair, use CTRL+C.");
    while(true) //começar o envio de mensagens
    {
        prompt = "@"+destinatario+">> ";
        System.out.print(prompt);
        String msg = sc.nextLine();
        if(!msg.isEmpty()) //se a mensagem não for vazia
        {
            char primeirochar = msg.charAt(0);
            int compare = Character.compare(primeirochar,'@');
            if(compare == 0) //se o primeiro caractere for '@', trocar o destinatário
            {
              destinatario = msg.substring(1);
              System.out.println("Enviando mensagem para "+destinatario+". Para trocar o destinatario digite '@<destinatario>'. Para sair, use CTRL+C.");
            }
            else //se não, enviar mensagem para o destinatário
            {
              /*
              * Montar o protocol buffer. O arquivo "MensagemProto.java" contém todas as 'messages' (classe Mensagem e classe Conteúdo).
              * Builders têm getters e setters. Servem para preencher o objeto. Usar .build() para transformar em uma Message.
              * Messages têm apenas getters e são imutáveis.
              */
              LocalDate data = java.time.LocalDate.now();
              int mes = data.getMonthValue();
              int dia = data.getDayOfMonth();
              String mes_formatado = mes < 10? "0"+String.valueOf(mes) : String.valueOf(mes);
              String dia_formatado = dia < 10? "0"+String.valueOf(dia) : String.valueOf(dia);
              String data_formatada = dia_formatado+"/"+mes_formatado+"/"+String.valueOf(data.getYear());
              LocalTime hora = java.time.LocalTime.now();
              String hora_formatada = String.valueOf(hora.getHour()) +":"+ String.valueOf(hora.getMinute());
              
              //Montar o conteúdo da mensagem (classe Conteúdo)
              MensagemProto.Conteudo.Builder conteudo = MensagemProto.Conteudo.newBuilder();
              conteudo.setTipo("text/plain")
                      .setCorpo(ByteString.copyFromUtf8(msg));
              
              //Montar a mensagem (classe Mensagem), setando o conteúdo para o montado anteriormente.
              MensagemProto.Mensagem.Builder mensagem = MensagemProto.Mensagem.newBuilder();
              MensagemProto.Mensagem mensagem_construida = 
                mensagem.setEmissor(USUARIO)
                        .setData(data_formatada)
                        .setHora(hora_formatada)
                        .setConteudo(conteudo)
                        .build();
                      
              //Serializar a Mensagem em um vetor de bytes para depois enviar
              byte[] mensagem_serializada = mensagem_construida.toByteArray();
              channel.basicPublish("",destinatario,null,mensagem_serializada);
            }
        }
    }
  }
}