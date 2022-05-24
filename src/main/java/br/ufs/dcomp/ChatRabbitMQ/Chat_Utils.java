package br.ufs.dcomp.ChatRabbitMQ;
import java.time.LocalDate;
import java.time.LocalTime;

public class Chat_Utils {
    
    /*
    * Função auxiliar para retornar a hora atual já formatada para a troca de mensagens
    */
    public static String getHora()
    {
      LocalTime hora = java.time.LocalTime.now();
      String hora_formatada = String.valueOf(hora.getHour()) +":"+ String.valueOf(hora.getMinute());
      return hora_formatada;
    }
    
    /*
    * Função auxiliar para retornar a data atual já formatada para a troca de mensagens
    */
    public static String getData()
    {
      LocalDate data = java.time.LocalDate.now();
      int mes = data.getMonthValue();
      int dia = data.getDayOfMonth();
      String mes_formatado = mes < 10? "0"+String.valueOf(mes) : String.valueOf(mes);
      String dia_formatado = dia < 10? "0"+String.valueOf(dia) : String.valueOf(dia);
      String data_formatada = dia_formatado+"/"+mes_formatado+"/"+String.valueOf(data.getYear());
      return data_formatada;
    }
    
    /*
    * Função auxiliar imprimir a lista de operações
    */
    public static void imprimirAjuda()
    {
      System.out.println("+----------------------------LISTA DE OPERAÇÕES-----------------------------+");
      System.out.println("| Digite '@<destinatário>' para enviar mensagem para uma pessoa. Ex: @joao  |");
      System.out.println("| Digite '!<comando>' para executar um comando. Ex: !addGroup amigos        |");
      System.out.println("| Digite '#<grupo>' para enviar mensagem para um grupo. Ex: #amigos         |");
      System.out.println("+---------------------------------------------------------------------------+");
    }
    
    /*
    * Função auxiliar imprimir a lista de comandos
    */
    public static void imprimirComandos()
    {
      System.out.println("/------------------------------------------------LISTA DE COMANDOS-------------------------------------------------\\");
      System.out.println("| Digite '!addGroup <nome_do_grupo>' para criar um novo grupo. Ex: !addGroup amigos                                |");
      System.out.println("| Digite '!addUser <usuario> <nome_do_grupo>' para adicionar um usuário a um grupo. Ex: !addUser joao amigos       |");
      System.out.println("| Digite '!delFromGroup <usuário> <nome_do_grupo>' para remover um usuário do grupo. Ex: !delFromGroup joao amigos |");
      System.out.println("| Digite '!removeGroup <nome_do_grupo>' para deletar um grupo. Ex: !removeGroup amigos                             |");
      System.out.println("| Digite '!upload <caminho_ate_o_arquivo>' para enviar um arquivo ao destinatário atual                            |");
      System.out.println("| Digite '!listUsers <nome_do_grupo>' para ver uma lista de todos os usuários do grupo                             |");
      System.out.println("| Digite '!listGroups' para ver uma lista dos grupos dos quais você faz parte                                      |");
      System.out.println("\\------------------------------------------------------------------------------------------------------------------/");
    }
}